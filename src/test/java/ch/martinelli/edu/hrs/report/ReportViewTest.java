package ch.martinelli.edu.hrs.report;

import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import ch.martinelli.edu.hrs.db.tables.records.RoomRecord;
import ch.martinelli.edu.hrs.room.RoomService;
import ch.martinelli.edu.hrs.room.RoomTypeService;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.select.Select;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.YearMonth;

import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static ch.martinelli.edu.hrs.report.ReportServiceTest.stay;
import static ch.martinelli.edu.hrs.room.RoomTypeServiceTest.roomType;
import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@WithMockUser(roles = "MANAGER")
class ReportViewTest extends HrsBrowserlessTest {

    @Autowired
    private RoomTypeService roomTypeService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private DSLContext dsl;

    @Autowired
    private Clock clock;

    @Test
    @SuppressWarnings("unchecked")
    void selectedMonthShowsOccupancyRevenueAndOnlineShare() {
        var month = YearMonth.now(clock).minusMonths(2);
        var type = roomTypeService.save(roomType("Test Report View", 2, "100.00"));
        var room = new RoomRecord();
        room.setRoomNumber("T861");
        room.setRoomTypeId(type.getId());
        var roomId = roomService.save(room).getId();
        stay(dsl, roomId, month.atDay(1), month.atDay(4), "CHECKED_OUT", "120.00", "ONLINE", month.atDay(1));
        navigate(ReportView.class);

        Select<YearMonth> picker = $(Select.class).withLabel("Month").single();
        assertThat(picker.getValue()).isEqualTo(YearMonth.now(clock));
        test(picker).selectItem(ReportView.label(month));

        var expected = BigDecimal.valueOf(300).divide(
                BigDecimal.valueOf((long) dsl.fetchCount(ROOM) * month.lengthOfMonth()), 1, RoundingMode.HALF_UP);
        assertThat(text("occupancy")).isEqualTo(expected + "%");
        assertThat(text("revenue")).isEqualTo("360.00");
        assertThat(text("online-share")).isEqualTo("100.0%");
    }

    @Test
    void monthWithoutNewReservationsShowsNoOnlineShare() {
        navigate(ReportView.class);

        test($(Select.class).withLabel("Month").single()).selectItem(ReportView.label(YearMonth.now(clock).minusMonths(30)));

        assertThat(text("online-share")).isEqualTo("–");
    }

    @Test
    @WithMockUser(roles = "RECEPTIONIST")
    void receptionistIsDenied() {
        assertThat(opens(ReportView.class)).isFalse();
    }

    @Test
    @WithMockUser(roles = "HOUSEKEEPING")
    void housekeepingIsDenied() {
        assertThat(opens(ReportView.class)).isFalse();
    }

    private String text(String id) {
        return $(Span.class).id(id).getText();
    }
}
