package ch.martinelli.edu.hrs.management.ui;

import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import ch.martinelli.edu.hrs.db.tables.records.SeasonRateRecord;
import ch.martinelli.edu.hrs.db.tables.records.SeasonRecord;
import ch.martinelli.edu.hrs.management.SeasonData;
import ch.martinelli.edu.hrs.management.SeasonOverlapException;
import ch.martinelli.edu.hrs.management.SeasonOverview;
import ch.martinelli.edu.hrs.management.SeasonService;
import ch.martinelli.edu.hrs.reservation.ReceptionReservationService;
import ch.martinelli.edu.hrs.reservation.ReceptionRoomType;
import ch.martinelli.edu.hrs.security.ui.LoginView;
import ch.martinelli.edu.hrs.usecase.UseCase;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.RouteNotFoundError;
import com.vaadin.flow.router.RouterLink;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.bean.override.convention.TestBean;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.RESERVATION_NIGHT;
import static ch.martinelli.edu.hrs.db.Tables.ROOM_TYPE;
import static ch.martinelli.edu.hrs.db.Tables.SEASON;
import static ch.martinelli.edu.hrs.db.Tables.SEASON_RATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Use case test for UC-025 "Maintain Seasonal Rates". Test data: V121__uc025_test_data.sql (inactive room type Riad
 * with the seasons Late May 2030 and Autumn 2030, which set season rates only for the Riad), room types from
 * V100__uc001_test_data.sql (active: Single Room 100.00, Double Room 150.00, Family Room 250.00); staff members from
 * V103__uc022_test_data.sql (password Secret-123). Today is 1 June 2030.
 */
@WithAnonymousUser
class UC025MaintainSeasonalRatesTest extends HrsBrowserlessTest {

    private static final long DOUBLE_ROOM = 1002;
    private static final long RIAD = 3301;
    private static final long AUTUMN_2030 = 3302;

    @TestBean
    Clock clock;

    @Autowired
    SeasonService seasonService;

    @Autowired
    ReceptionReservationService receptionReservationService;

    @Autowired
    DSLContext ctx;

    private Result<SeasonRecord> originalSeasons;
    private Result<SeasonRateRecord> originalSeasonRates;

    static Clock clock() {
        return Clock.fixed(Instant.parse("2030-06-01T10:00:00Z"), ZoneId.of("Europe/Zurich"));
    }

    @BeforeEach
    void rememberTestData() {
        originalSeasons = ctx.selectFrom(SEASON).fetch();
        originalSeasonRates = ctx.selectFrom(SEASON_RATE).fetch();
    }

    @AfterEach
    void restoreTestData() {
        SecurityContextHolder.clearContext();
        // Seasons created, changed or deleted by the tests are replaced by the test data
        ctx.deleteFrom(SEASON_RATE).execute();
        ctx.deleteFrom(SEASON).execute();
        originalSeasons.forEach(season -> season.changed(true));
        originalSeasonRates.forEach(rate -> rate.changed(true));
        ctx.batchInsert(originalSeasons).execute();
        ctx.batchInsert(originalSeasonRates).execute();
        ctx.update(ROOM_TYPE).set(ROOM_TYPE.ACTIVE, false).where(ROOM_TYPE.ID.eq(RIAD)).execute();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Main success scenario
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-025", businessRules = {"BR-002", "BR-003", "BR-006"})
    void manager_creates_a_season_and_searches_use_its_season_rates() {
        logIn("mark");

        // Step 1
        test($(RouterLink.class).withText("Seasons").single()).click();
        assertThat(getCurrentView()).isInstanceOf(SeasonsView.class);

        // Step 2: all seasons ordered by first night, with name, first night, last night and number of room types
        Grid<SeasonOverview> grid = grid();
        assertThat(grid.getColumns()).extracting(Grid.Column::getHeaderText)
                .containsExactly("Name", "First night", "Last night", "Room types with season rate");
        assertThat(names()).containsExactly("Late May 2030", "Autumn 2030");
        assertThat(row("Late May 2030")).containsExactly("Late May 2030", "May 30, 2030", "May 31, 2030", "1");
        assertThat(row("Autumn 2030")).containsExactly("Autumn 2030", "Nov 5, 2030", "Nov 7, 2030", "1");

        // Step 3
        test($(Button.class).withText("New season").single()).click();
        // Step 4: every active room type with its normal rate (BR-006)
        assertThat(rateFields()).extracting(BigDecimalField::getLabel)
                .containsExactly("Double Room", "Family Room", "Single Room");
        assertThat(rateField("Double Room").getHelperText()).isEqualTo("Normal rate CHF 150.00");
        assertThat(rateField("Double Room").getValue()).isNull();

        // Step 5: the Single Room is left without a season rate
        test(nameField()).setValue("Christmas 2030");
        test(firstNight()).setValue(LocalDate.of(2030, 12, 20));
        test(lastNight()).setValue(LocalDate.of(2031, 1, 2));
        test(rateField("Double Room")).setValue(new BigDecimal("210.00"));
        test(rateField("Family Room")).setValue(new BigDecimal("320.00"));
        // Step 6
        save();

        // Step 9: the updated list
        assertThat(message()).isEqualTo("Season Christmas 2030 is saved.");
        assertThat(names()).containsExactly("Late May 2030", "Autumn 2030", "Christmas 2030");
        assertThat(row("Christmas 2030")).containsExactly("Christmas 2030", "Dec 20, 2030", "Jan 2, 2031", "2");

        // Step 8 / success postcondition
        SeasonRecord saved = ctx.selectFrom(SEASON).where(SEASON.NAME.eq("Christmas 2030")).fetchSingle();
        assertThat(saved.getFirstNight()).isEqualTo(LocalDate.of(2030, 12, 20));
        assertThat(saved.getLastNight()).isEqualTo(LocalDate.of(2031, 1, 2));
        assertThat(seasonRates(saved.getId())).containsOnlyKeys(DOUBLE_ROOM, 1003L);
        assertThat(seasonRates(saved.getId()).get(DOUBLE_ROOM)).isEqualByComparingTo("210.00");

        // BR-003: the night of 19 December has the normal rate, the night of 20 December the season rate
        ReceptionRoomType doubleRoom = offer(LocalDate.of(2030, 12, 19), LocalDate.of(2030, 12, 21), "Double Room");
        assertThat(doubleRoom.lowestNightlyRate()).isEqualByComparingTo("150.00");
        assertThat(doubleRoom.highestNightlyRate()).isEqualByComparingTo("210.00");
        assertThat(doubleRoom.totalPrice()).isEqualByComparingTo("360.00");
        // BR-003: a room type without a season rate keeps its normal rate during the season
        ReceptionRoomType singleRoom = offer(LocalDate.of(2030, 12, 19), LocalDate.of(2030, 12, 21), "Single Room");
        assertThat(singleRoom.highestNightlyRate()).isEqualByComparingTo("100.00");
        assertThat(singleRoom.totalPrice()).isEqualByComparingTo("200.00");
    }

    @Test
    @UseCase(id = "UC-025", businessRules = {"BR-002", "BR-003"})
    void last_night_of_a_season_is_priced_at_the_season_rate() {
        openSeasons();
        createSeason("Christmas 2030", LocalDate.of(2030, 12, 20), LocalDate.of(2030, 12, 20),
                Map.of("Double Room", "210.00"));

        // BR-002: a night belongs to the date on which it begins, so 20 December is in the season, 21 December not
        ReceptionRoomType doubleRoom = offer(LocalDate.of(2030, 12, 20), LocalDate.of(2030, 12, 22), "Double Room");
        assertThat(doubleRoom.totalPrice()).isEqualByComparingTo("360.00");
    }

    @Test
    @UseCase(id = "UC-025", businessRules = {"BR-004", "BR-006"})
    void manager_changes_a_season_and_existing_reservations_keep_their_prices() {
        openSeasons();

        // Step 3: select an existing season
        select("Autumn 2030");
        // Step 4: the inactive Riad is shown because it already has a season rate in the season (BR-006)
        assertThat(rateFields()).extracting(BigDecimalField::getLabel)
                .containsExactly("Double Room", "Family Room", "Riad", "Single Room");
        assertThat(rateField("Riad").getValue()).isEqualByComparingTo("160.00");
        assertThat(rateField("Riad").getHelperText()).isEqualTo("Normal rate CHF 100.00 · inactive room type");
        assertThat(nameField().getValue()).isEqualTo("Autumn 2030");
        assertThat(firstNight().getValue()).isEqualTo(LocalDate.of(2030, 11, 5));
        assertThat(lastNight().getValue()).isEqualTo(LocalDate.of(2030, 11, 7));

        // Step 5: an existing season rate of an inactive room type can be changed (BR-006)
        test(nameField()).setValue("Autumn Break 2030");
        test(lastNight()).setValue(LocalDate.of(2030, 11, 8));
        test(rateField("Riad")).setValue(new BigDecimal("170.00"));
        test(rateField("Double Room")).setValue(new BigDecimal("175.00"));
        save();

        assertThat(message()).isEqualTo("Season Autumn Break 2030 is saved.");
        assertThat(row("Autumn Break 2030")).containsExactly("Autumn Break 2030", "Nov 5, 2030", "Nov 8, 2030", "2");
        assertThat(seasonRates(AUTUMN_2030).get(RIAD)).isEqualByComparingTo("170.00");
        assertThat(seasonRates(AUTUMN_2030).get(DOUBLE_ROOM)).isEqualByComparingTo("175.00");

        // BR-004: the reservation recorded for nights of the season keeps its night prices and total price
        assertThat(nightPrices("RIAD000004")).containsExactly(new BigDecimal("100.00"), new BigDecimal("100.00"));
        assertThat(totalPrice("RIAD000004")).isEqualByComparingTo("200.00");
    }

    @Test
    @UseCase(id = "UC-025", businessRules = {"BR-003", "BR-004"})
    void new_season_over_an_existing_reservation_leaves_its_prices_unchanged() {
        // The Riad must be active to get a season rate (BR-006)
        ctx.update(ROOM_TYPE).set(ROOM_TYPE.ACTIVE, true).where(ROOM_TYPE.ID.eq(RIAD)).execute();
        openSeasons();

        // RIAD000003: 3 and 4 November, recorded at 90.00 per night
        createSeason("Early November 2030", LocalDate.of(2030, 11, 1), LocalDate.of(2030, 11, 4),
                Map.of("Riad", "200.00"));

        // BR-004 / success postcondition: the existing reservation keeps its night prices and total price
        assertThat(nightPrices("RIAD000003")).containsExactly(new BigDecimal("90.00"), new BigDecimal("90.00"));
        assertThat(totalPrice("RIAD000003")).isEqualByComparingTo("180.00");
        // BR-003: new searches for the same nights use the season rate
        assertThat(offer(LocalDate.of(2030, 11, 3), LocalDate.of(2030, 11, 5), "Riad").totalPrice())
                .isEqualByComparingTo("400.00");
    }

    @Test
    @UseCase(id = "UC-025", businessRules = {"BR-006"})
    void season_rate_of_an_inactive_room_type_can_be_removed_but_not_added() {
        openSeasons();
        select("Autumn 2030");

        // Removed: the Riad keeps its normal rate during the season
        test(rateField("Riad")).clear();
        save();
        assertThat(row("Autumn 2030")).containsExactly("Autumn 2030", "Nov 5, 2030", "Nov 7, 2030", "0");
        assertThat(seasonRates(AUTUMN_2030)).isEmpty();

        // Once removed, the inactive Riad is no longer shown, so its season rate cannot be added again
        select("Autumn 2030");
        assertThat(rateFields()).extracting(BigDecimalField::getLabel)
                .containsExactly("Double Room", "Family Room", "Single Room");
        assertThatThrownBy(() -> seasonService.save(AUTUMN_2030, new SeasonData("Autumn 2030",
                LocalDate.of(2030, 11, 5), LocalDate.of(2030, 11, 7), Map.of(RIAD, new BigDecimal("160.00")))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(seasonRates(AUTUMN_2030)).isEmpty();
    }

    @Test
    @UseCase(id = "UC-025", businessRules = {"BR-006"})
    void new_season_shows_only_active_room_types() {
        openSeasons();
        test($(Button.class).withText("New season").single()).click();

        // Inactive room types without a season rate (Suite, Riad and others) are not shown
        assertThat(rateFields()).extracting(BigDecimalField::getLabel)
                .containsExactly("Double Room", "Family Room", "Single Room");
        assertThatThrownBy(() -> seasonService.save(null, new SeasonData("Riad Winter 2030",
                LocalDate.of(2030, 12, 1), LocalDate.of(2030, 12, 5), Map.of(RIAD, new BigDecimal("80.00")))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(ctx.fetchExists(SEASON, SEASON.NAME.eq("Riad Winter 2030"))).isFalse();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A1: Invalid Values
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-025", scenario = "A1: Invalid Values")
    void missing_name_and_nights_are_marked_and_nothing_is_saved() {
        openSeasons();
        test($(Button.class).withText("New season").single()).click();

        save();

        assertThat(nameField().isInvalid()).isTrue();
        assertThat(nameField().getErrorMessage()).isEqualTo("Enter the name");
        assertThat(firstNight().isInvalid()).isTrue();
        assertThat(firstNight().getErrorMessage()).isEqualTo("Enter the first night");
        assertThat(lastNight().isInvalid()).isTrue();
        assertThat(lastNight().getErrorMessage()).isEqualTo("Enter the last night");
        assertThat(ctx.fetchCount(SEASON)).isEqualTo(2);
    }

    @Test
    @UseCase(id = "UC-025", scenario = "A1: Invalid Values", businessRules = {"BR-006"})
    void last_night_before_first_night_and_invalid_season_rates_are_marked_and_the_values_are_kept() {
        openSeasons();
        test($(Button.class).withText("New season").single()).click();

        test(nameField()).setValue("Christmas 2030");
        test(firstNight()).setValue(LocalDate.of(2030, 12, 20));
        test(lastNight()).setValue(LocalDate.of(2030, 12, 19));
        test(rateField("Double Room")).setValue(new BigDecimal("-1.00"));
        test(rateField("Family Room")).setValue(new BigDecimal("100000000.00"));
        save();

        // A1 step 1: marked, the entered values are kept
        assertThat(lastNight().isInvalid()).isTrue();
        assertThat(lastNight().getErrorMessage()).isEqualTo("The last night must be on or after the first night");
        assertThat(rateField("Double Room").isInvalid()).isTrue();
        assertThat(rateField("Double Room").getErrorMessage()).isEqualTo(SeasonsView.RATE_INVALID);
        assertThat(rateField("Family Room").isInvalid()).isTrue();
        assertThat(nameField().getValue()).isEqualTo("Christmas 2030");
        assertThat(rateField("Double Room").getValue()).isEqualByComparingTo("-1.00");
        assertThat(ctx.fetchExists(SEASON, SEASON.NAME.eq("Christmas 2030"))).isFalse();
        // Failure postcondition: no existing reservation is changed
        assertThat(totalPrice("RIAD000004")).isEqualByComparingTo("200.00");

        // A1 step 2: continues at step 5; 0 and 99,999,999.99 are valid season rates
        test(lastNight()).setValue(LocalDate.of(2030, 12, 20));
        test(rateField("Double Room")).setValue(new BigDecimal("0.00"));
        test(rateField("Family Room")).setValue(new BigDecimal("99999999.99"));
        save();
        assertThat(message()).isEqualTo("Season Christmas 2030 is saved.");
    }

    @Test
    @UseCase(id = "UC-025", scenario = "A1: Invalid Values")
    void name_longer_than_50_characters_is_refused() {
        openSeasons();
        test($(Button.class).withText("New season").single()).click();

        assertThat(nameField().getMaxLength()).isEqualTo(50);
        assertThatThrownBy(() -> seasonService.save(null, new SeasonData("x".repeat(51),
                LocalDate.of(2030, 12, 20), LocalDate.of(2030, 12, 21), Map.of())))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(seasonService.save(null, new SeasonData("x".repeat(50), LocalDate.of(2030, 12, 20),
                LocalDate.of(2030, 12, 21), Map.of()))).isNotNull();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A2: Name Already Used
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-025", scenario = "A2: Name Already Used", businessRules = {"BR-001"})
    void name_of_another_season_is_refused() {
        openSeasons();
        test($(Button.class).withText("New season").single()).click();

        test(nameField()).setValue("Autumn 2030");
        test(firstNight()).setValue(LocalDate.of(2030, 12, 20));
        test(lastNight()).setValue(LocalDate.of(2030, 12, 21));
        save();

        // A2 step 1
        assertThat(message()).isEqualTo(SeasonsView.NAME_USED);
        assertThat(nameField().isInvalid()).isTrue();
        assertThat(ctx.fetchCount(SEASON)).isEqualTo(2);

        // A2 step 2: continues at step 5
        test(nameField()).setValue("Christmas 2030");
        save();
        assertThat(message()).isEqualTo("Season Christmas 2030 is saved.");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A3: Seasons Overlap
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-025", scenario = "A3: Seasons Overlap", businessRules = {"BR-002"})
    void season_with_a_night_of_another_season_is_refused() {
        openSeasons();
        test($(Button.class).withText("New season").single()).click();

        test(nameField()).setValue("November 2030");
        test(firstNight()).setValue(LocalDate.of(2030, 11, 7));
        test(lastNight()).setValue(LocalDate.of(2030, 11, 30));
        save();

        // A3 step 1: names the other season and its first and last night
        assertThat(message()).isEqualTo("The season overlaps the season Autumn 2030 from Nov 5, 2030 to Nov 7, 2030.");
        assertThat(ctx.fetchExists(SEASON, SEASON.NAME.eq("November 2030"))).isFalse();
        // Failure postconditions: no new season, the other season keeps its data, no reservation is changed
        assertThat(row("Autumn 2030")).containsExactly("Autumn 2030", "Nov 5, 2030", "Nov 7, 2030", "1");
        assertThat(seasonRates(AUTUMN_2030).get(RIAD)).isEqualByComparingTo("160.00");
        assertThat(nightPrices("RIAD000004")).containsExactly(new BigDecimal("100.00"), new BigDecimal("100.00"));
        assertThat(totalPrice("RIAD000004")).isEqualByComparingTo("200.00");

        // A3 step 2: continues at step 5; the night after the last night of Autumn 2030 is free
        test(firstNight()).setValue(LocalDate.of(2030, 11, 8));
        save();
        assertThat(message()).isEqualTo("Season November 2030 is saved.");
    }

    @Test
    @UseCase(id = "UC-025", scenario = "A3: Seasons Overlap", businessRules = {"BR-002"})
    void season_covering_another_season_completely_is_refused() {
        assertThatThrownBy(() -> seasonService.save(null, new SeasonData("Year 2030", LocalDate.of(2030, 1, 1),
                LocalDate.of(2030, 12, 31), Map.of())))
                .isInstanceOf(SeasonOverlapException.class)
                .extracting(e -> ((SeasonOverlapException) e).otherSeason().name())
                .isEqualTo("Late May 2030");
        // A season does not overlap itself when its own nights change
        assertThat(seasonService.save(AUTUMN_2030, new SeasonData("Autumn 2030", LocalDate.of(2030, 11, 4),
                LocalDate.of(2030, 11, 6), Map.of()))).isEqualTo(AUTUMN_2030);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A4: Manager Deletes the Season
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-025", scenario = "A4: Manager Deletes the Season", businessRules = {"BR-004", "BR-005"})
    void manager_deletes_a_season_and_its_nights_use_the_normal_rate() {
        ctx.update(ROOM_TYPE).set(ROOM_TYPE.ACTIVE, true).where(ROOM_TYPE.ID.eq(RIAD)).execute();
        // Before: 5 and 6 November at the season rate
        assertThat(offer(LocalDate.of(2030, 11, 5), LocalDate.of(2030, 11, 7), "Riad").totalPrice())
                .isEqualByComparingTo("320.00");
        openSeasons();
        select("Autumn 2030");

        // A4 step 1: the manager is asked to confirm
        test($(Button.class).withText("Delete").single()).click();
        ConfirmDialog dialog = $(ConfirmDialog.class).single();
        assertThat(dialog.isOpened()).isTrue();
        // A4 step 2
        test(dialog).confirm();

        // A4 steps 3 and 4: removed with its season rates, the updated list
        assertThat(message()).isEqualTo("Season Autumn 2030 is deleted.");
        assertThat(names()).containsExactly("Late May 2030");
        assertThat(ctx.fetchExists(SEASON_RATE, SEASON_RATE.SEASON_ID.eq(AUTUMN_2030))).isFalse();
        // BR-005: its nights use the normal rate for nights recorded afterwards
        assertThat(offer(LocalDate.of(2030, 11, 5), LocalDate.of(2030, 11, 7), "Riad").totalPrice())
                .isEqualByComparingTo("200.00");
        // BR-004: the reservation for nights of the deleted season keeps its night prices and total price
        assertThat(nightPrices("RIAD000004")).containsExactly(new BigDecimal("100.00"), new BigDecimal("100.00"));
        assertThat(totalPrice("RIAD000004")).isEqualByComparingTo("200.00");
    }

    @Test
    @UseCase(id = "UC-025", scenario = "A4: Manager Deletes the Season", businessRules = {"BR-004", "BR-005"})
    void season_that_has_ended_can_be_deleted() {
        // Late May 2030 has ended: today is 1 June 2030
        openSeasons();
        select("Late May 2030");
        test($(Button.class).withText("Delete").single()).click();
        test($(ConfirmDialog.class).single()).confirm();

        assertThat(names()).containsExactly("Autumn 2030");
        // BR-004: the nights recorded at the season rate keep it
        assertThat(nightPrices("RIAD000002")).containsExactly(new BigDecimal("100.00"), new BigDecimal("180.00"),
                new BigDecimal("180.00"), new BigDecimal("100.00"), new BigDecimal("100.00"), new BigDecimal("100.00"));
        assertThat(totalPrice("RIAD000002")).isEqualByComparingTo("760.00");
    }

    @Test
    @UseCase(id = "UC-025", scenario = "A4: Manager Deletes the Season")
    void new_season_cannot_be_deleted() {
        openSeasons();
        test($(Button.class).withText("New season").single()).click();

        assertThat($(Button.class).withText("Delete").exists()).isFalse();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A5: Manager Discards the Changes
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-025", scenario = "A5: Manager Discards the Changes")
    void discarded_changes_are_not_saved() {
        openSeasons();
        select("Autumn 2030");
        test(nameField()).setValue("Changed");
        test(rateField("Riad")).setValue(new BigDecimal("999.00"));

        test($(Button.class).withText("Discard").single()).click();

        // A5 step 1 and failure postcondition: the season keeps its data and season rates
        assertThat($(TextField.class).withCaption("Name").exists()).isFalse();
        assertThat(names()).containsExactly("Late May 2030", "Autumn 2030");
        assertThat(seasonRates(AUTUMN_2030).get(RIAD)).isEqualByComparingTo("160.00");
        // Selecting it again shows the stored values
        select("Autumn 2030");
        assertThat(nameField().getValue()).isEqualTo("Autumn 2030");
        assertThat(rateField("Riad").getValue()).isEqualByComparingTo("160.00");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Precondition: the manager is logged in (NFR-009)
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-025")
    void only_a_logged_in_manager_can_maintain_seasonal_rates() {
        navigate("seasons", LoginView.class);

        logIn("rita");
        assertThat($(RouterLink.class).withText("Seasons").exists()).isFalse();
        assertThat(navigate("seasons", RouteNotFoundError.class)).isNotNull();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------------------------

    private void logIn(String username) {
        navigate(LoginView.class);
        test($(TextField.class).withCaption("Username").single()).setValue(username);
        test($(PasswordField.class).withCaption("Password").single()).setValue("Secret-123");
        test($(Button.class).withText("Log in").single()).click();
    }

    private void openSeasons() {
        logIn("mark");
        navigate(SeasonsView.class);
    }

    private void createSeason(String name, LocalDate first, LocalDate last, Map<String, String> rates) {
        test($(Button.class).withText("New season").single()).click();
        test(nameField()).setValue(name);
        test(firstNight()).setValue(first);
        test(lastNight()).setValue(last);
        rates.forEach((roomType, rate) -> test(rateField(roomType)).setValue(new BigDecimal(rate)));
        save();
        assertThat(message()).isEqualTo("Season " + name + " is saved.");
    }

    @SuppressWarnings("unchecked")
    private Grid<SeasonOverview> grid() {
        return $(Grid.class).withId("seasons").single();
    }

    private List<String> names() {
        Grid<SeasonOverview> grid = grid();
        return IntStream.range(0, test(grid).size()).mapToObj(index -> test(grid).getRow(index).name()).toList();
    }

    private int rowIndex(String name) {
        int index = names().indexOf(name);
        if (index < 0) {
            throw new AssertionError(name + " is not listed");
        }
        return index;
    }

    private List<String> row(String name) {
        Grid<SeasonOverview> grid = grid();
        int index = rowIndex(name);
        return IntStream.range(0, grid.getColumns().size()).mapToObj(column -> test(grid).getCellText(index, column))
                .toList();
    }

    private void select(String name) {
        test(grid()).select(rowIndex(name));
    }

    private TextField nameField() {
        return $(TextField.class).withCaption("Name").single();
    }

    private DatePicker firstNight() {
        return $(DatePicker.class).withCaption("First night").single();
    }

    private DatePicker lastNight() {
        return $(DatePicker.class).withCaption("Last night").single();
    }

    private List<BigDecimalField> rateFields() {
        return $(BigDecimalField.class).all();
    }

    private BigDecimalField rateField(String roomTypeName) {
        return $(BigDecimalField.class).withCaption(roomTypeName).single();
    }

    private void save() {
        test($(Button.class).withText("Save").single()).click();
    }

    private String message() {
        return $(Paragraph.class).withId("season-message").single().getText();
    }

    private Map<Long, BigDecimal> seasonRates(long seasonId) {
        return ctx.select(SEASON_RATE.ROOM_TYPE_ID, SEASON_RATE.NIGHTLY_RATE).from(SEASON_RATE)
                .where(SEASON_RATE.SEASON_ID.eq(seasonId))
                .fetchMap(SEASON_RATE.ROOM_TYPE_ID, SEASON_RATE.NIGHTLY_RATE);
    }

    private ReceptionRoomType offer(LocalDate arrival, LocalDate departure, String name) {
        return receptionReservationService.searchAvailableRoomTypes(arrival, departure, 1).stream()
                .filter(roomType -> roomType.name().equals(name))
                .findFirst().orElseThrow(() -> new AssertionError(name + " is not offered"));
    }

    private List<BigDecimal> nightPrices(String reservationCode) {
        return ctx.select(RESERVATION_NIGHT.NIGHT_PRICE).from(RESERVATION_NIGHT)
                .join(RESERVATION).on(RESERVATION.ID.eq(RESERVATION_NIGHT.RESERVATION_ID))
                .where(RESERVATION.RESERVATION_CODE.eq(reservationCode))
                .orderBy(RESERVATION_NIGHT.NIGHT_DATE)
                .fetch(RESERVATION_NIGHT.NIGHT_PRICE);
    }

    private BigDecimal totalPrice(String reservationCode) {
        return ctx.select(RESERVATION.TOTAL_PRICE).from(RESERVATION)
                .where(RESERVATION.RESERVATION_CODE.eq(reservationCode)).fetchSingle(RESERVATION.TOTAL_PRICE);
    }
}
