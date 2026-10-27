package ch.martinelli.edu.hrs.management;

import org.jooq.DSLContext;
import org.jooq.Records;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static ch.martinelli.edu.hrs.db.Tables.ROOM_TYPE;
import static ch.martinelli.edu.hrs.db.Tables.SEASON;
import static ch.martinelli.edu.hrs.db.Tables.SEASON_RATE;
import static org.jooq.impl.DSL.coalesce;
import static org.jooq.impl.DSL.count;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.select;

/**
 * Reads and records seasons and their season rates (UC-025 steps 2, 4 and 8, A4).
 */
@Repository
public class SeasonRepository {

    private final DSLContext ctx;

    public SeasonRepository(DSLContext ctx) {
        this.ctx = ctx;
    }

    /**
     * All seasons ordered by first night, each with the number of room types with a season rate.
     */
    public List<SeasonOverview> findAll() {
        return ctx.select(SEASON.ID, SEASON.NAME, SEASON.FIRST_NIGHT, SEASON.LAST_NIGHT,
                        coalesce(field(select(count()).from(SEASON_RATE)
                                .where(SEASON_RATE.SEASON_ID.eq(SEASON.ID))), 0))
                .from(SEASON)
                .orderBy(SEASON.FIRST_NIGHT, SEASON.NAME)
                .fetch(Records.mapping(SeasonOverview::new));
    }

    public Optional<SeasonOverview> findById(Long seasonId) {
        return ctx.select(SEASON.ID, SEASON.NAME, SEASON.FIRST_NIGHT, SEASON.LAST_NIGHT,
                        coalesce(field(select(count()).from(SEASON_RATE)
                                .where(SEASON_RATE.SEASON_ID.eq(SEASON.ID))), 0))
                .from(SEASON)
                .where(SEASON.ID.eq(seasonId))
                .fetchOptional(Records.mapping(SeasonOverview::new));
    }

    /**
     * The room types of a season ({@code seasonId} null for a new season), ordered by name.
     */
    // UC-025 BR-006: Every active room type and every inactive room type with a season rate in the season.
    public List<SeasonRoomType> findRoomTypes(Long seasonId) {
        var seasonRate = SEASON_RATE.as("sr");
        return ctx.select(ROOM_TYPE.ID, ROOM_TYPE.NAME, ROOM_TYPE.ACTIVE, ROOM_TYPE.NIGHTLY_RATE,
                        seasonRate.NIGHTLY_RATE)
                .from(ROOM_TYPE)
                .leftJoin(seasonRate).on(seasonRate.ROOM_TYPE_ID.eq(ROOM_TYPE.ID)
                        .and(seasonId == null ? seasonRate.SEASON_ID.isNull() : seasonRate.SEASON_ID.eq(seasonId)))
                .where(ROOM_TYPE.ACTIVE.isTrue().or(seasonRate.NIGHTLY_RATE.isNotNull()))
                .orderBy(ROOM_TYPE.NAME)
                .fetch(Records.mapping(SeasonRoomType::new));
    }

    /**
     * Whether each of the room types is active, by room type id; unknown room types are missing.
     */
    public Map<Long, Boolean> findActive(Collection<Long> roomTypeIds) {
        return ctx.select(ROOM_TYPE.ID, ROOM_TYPE.ACTIVE)
                .from(ROOM_TYPE)
                .where(ROOM_TYPE.ID.in(roomTypeIds))
                .fetchMap(ROOM_TYPE.ID, ROOM_TYPE.ACTIVE);
    }

    public Map<Long, BigDecimal> findSeasonRates(Long seasonId) {
        return ctx.select(SEASON_RATE.ROOM_TYPE_ID, SEASON_RATE.NIGHTLY_RATE)
                .from(SEASON_RATE)
                .where(SEASON_RATE.SEASON_ID.eq(seasonId))
                .fetchMap(SEASON_RATE.ROOM_TYPE_ID, SEASON_RATE.NIGHTLY_RATE);
    }

    public boolean exists(Long seasonId) {
        return ctx.fetchExists(SEASON, SEASON.ID.eq(seasonId));
    }

    // UC-025 BR-001: Every season has a name that no other season has.
    public boolean existsOtherWithName(String name, Long excludedId) {
        return ctx.fetchExists(SEASON, SEASON.NAME.eq(name)
                .and(excludedId == null ? SEASON.ID.isNotNull() : SEASON.ID.ne(excludedId)));
    }

    /**
     * The first other season, by first night, that covers at least one night from {@code firstNight} to
     * {@code lastNight}.
     */
    // UC-025 BR-002: Every night belongs to at most one season.
    public Optional<SeasonOverview> findOverlapping(LocalDate firstNight, LocalDate lastNight, Long excludedId) {
        return ctx.select(SEASON.ID, SEASON.NAME, SEASON.FIRST_NIGHT, SEASON.LAST_NIGHT,
                        coalesce(field(select(count()).from(SEASON_RATE)
                                .where(SEASON_RATE.SEASON_ID.eq(SEASON.ID))), 0))
                .from(SEASON)
                .where(SEASON.FIRST_NIGHT.le(lastNight))
                .and(SEASON.LAST_NIGHT.ge(firstNight))
                .and(excludedId == null ? SEASON.ID.isNotNull() : SEASON.ID.ne(excludedId))
                .orderBy(SEASON.FIRST_NIGHT)
                .limit(1)
                .fetchOptional(Records.mapping(SeasonOverview::new));
    }

    public Long insert(SeasonData data) {
        Long seasonId = ctx.insertInto(SEASON)
                .set(SEASON.NAME, data.name())
                .set(SEASON.FIRST_NIGHT, data.firstNight())
                .set(SEASON.LAST_NIGHT, data.lastNight())
                .returning(SEASON.ID)
                .fetchSingle(SEASON.ID);
        insertRates(seasonId, data.seasonRates());
        return seasonId;
    }

    public void update(Long seasonId, SeasonData data) {
        ctx.update(SEASON)
                .set(SEASON.NAME, data.name())
                .set(SEASON.FIRST_NIGHT, data.firstNight())
                .set(SEASON.LAST_NIGHT, data.lastNight())
                .where(SEASON.ID.eq(seasonId))
                .execute();
        ctx.deleteFrom(SEASON_RATE).where(SEASON_RATE.SEASON_ID.eq(seasonId)).execute();
        insertRates(seasonId, data.seasonRates());
    }

    // UC-025 BR-005: Deleting a season removes it and its season rates.
    public void delete(Long seasonId) {
        ctx.deleteFrom(SEASON_RATE).where(SEASON_RATE.SEASON_ID.eq(seasonId)).execute();
        ctx.deleteFrom(SEASON).where(SEASON.ID.eq(seasonId)).execute();
    }

    private void insertRates(Long seasonId, Map<Long, BigDecimal> rates) {
        rates.forEach((roomTypeId, rate) -> ctx.insertInto(SEASON_RATE)
                .set(SEASON_RATE.SEASON_ID, seasonId)
                .set(SEASON_RATE.ROOM_TYPE_ID, roomTypeId)
                .set(SEASON_RATE.NIGHTLY_RATE, rate)
                .execute());
    }
}
