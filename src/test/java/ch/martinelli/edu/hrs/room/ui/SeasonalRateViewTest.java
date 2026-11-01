package ch.martinelli.edu.hrs.room.ui;

import ch.martinelli.edu.hrs.core.ui.AccessDeniedView;
import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import ch.martinelli.edu.hrs.db.tables.records.RoomTypeRecord;
import ch.martinelli.edu.hrs.db.tables.records.SeasonalRateRecord;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import static ch.martinelli.edu.hrs.reservation.ReservationTestData.*;
import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@WithUserDetails("manager")
class SeasonalRateViewTest extends HrsBrowserlessTest {

    static final LocalDate JULY_1 = LocalDate.of(2030, 7, 1);
    static final LocalDate AUGUST_31 = LocalDate.of(2030, 8, 31);

    @Autowired
    DSLContext ctx;

    Grid<SeasonalRateRecord> grid;
    ComboBox<RoomTypeRecord> roomType;
    TextField name;
    DatePicker firstNight;
    DatePicker lastNight;
    BigDecimalField nightlyRate;

    @SuppressWarnings("unchecked")
    private void open() {
        navigate(SeasonalRateView.class);
        grid = $(Grid.class).id("seasonal-rates");
        roomType = $(ComboBox.class).withLabel("Room type").single();
        name = $(TextField.class).withLabel("Name").single();
        firstNight = $(DatePicker.class).withLabel("First night").single();
        lastNight = $(DatePicker.class).withLabel("Last night").single();
        nightlyRate = $(BigDecimalField.class).withLabel("Nightly rate").single();
    }

    // Scenario "Manager lists seasonal rates"
    @Test
    void lists_seasonal_rates_by_room_type_and_first_night() {
        insertSeasonalRate(ctx, "Family", "Christmas", DECEMBER_20, JANUARY_6, "280.00");
        insertSeasonalRate(ctx, "Double", "Christmas", DECEMBER_20, JANUARY_6, "180.00");
        insertSeasonalRate(ctx, "Double", "Summer", JULY_1, AUGUST_31, "170.00");
        open();

        assertThat(rows()).containsExactly(
                "Double|Summer|2030-07-01|2030-08-31|CHF 170.00",
                "Double|Christmas|2030-12-20|2031-01-06|CHF 180.00",
                "Family|Christmas|2030-12-20|2031-01-06|CHF 280.00");
    }

    // Scenario "Seasonal rate created"
    @Test
    void creates_seasonal_rate() {
        open();
        fill("Double", "Christmas", DECEMBER_20, JANUARY_6, "180.00");
        save();

        assertThat(rows()).containsExactly("Double|Christmas|2030-12-20|2031-01-06|CHF 180.00");
    }

    // Scenario "Missing or invalid seasonal rate values"
    @Test
    void missing_values_mark_the_fields() {
        open();
        save();

        assertThat(roomType.getErrorMessage()).isEqualTo("Room type is required");
        assertThat(name.getErrorMessage()).isEqualTo("Name is required");
        assertThat(firstNight.getErrorMessage()).isEqualTo("First night is required");
        assertThat(lastNight.getErrorMessage()).isEqualTo("Last night is required");
        assertThat(nightlyRate.getErrorMessage()).isEqualTo("Nightly rate is required");
        assertThat(List.of(roomType.isInvalid(), name.isInvalid(), firstNight.isInvalid(), lastNight.isInvalid(),
                nightlyRate.isInvalid())).containsOnly(true);
        assertThat(test(grid).size()).isZero();
    }

    // Scenario "Missing or invalid seasonal rate values": rate of zero or with more than two decimals
    @Test
    void invalid_rate_marks_the_field() {
        open();
        fill("Double", "Christmas", DECEMBER_20, JANUARY_6, "0.00");
        save();
        assertThat(nightlyRate.getErrorMessage()).isEqualTo("Nightly rate must be greater than zero");

        nightlyRate.setValue(new BigDecimal("180.005"));
        save();
        assertThat(nightlyRate.getErrorMessage()).isEqualTo("Nightly rate has at most two decimals");
        assertThat(test(grid).size()).isZero();
    }

    // Scenario "Last night before first night"
    @Test
    void last_night_before_first_night_shows_message() {
        open();
        fill("Double", "Christmas", JANUARY_6, DECEMBER_20, "180.00");
        save();

        assertThat(lastNight.isInvalid()).isTrue();
        assertThat(lastNight.getErrorMessage()).isEqualTo("Last night cannot be before first night");
        assertThat(test(grid).size()).isZero();
    }

    // Scenario "Overlapping seasonal rate"
    @Test
    void overlapping_seasonal_rate_shows_message() {
        insertSeasonalRate(ctx, "Double", "Christmas", DECEMBER_20, JANUARY_6, "180.00");
        open();
        fill("Double", "January", LocalDate.of(2031, 1, 1), LocalDate.of(2031, 1, 15), "120.00");
        save();

        assertThat(test($(Notification.class).last()).getText())
                .isEqualTo("The dates overlap the seasonal rate \"Christmas\" of this room type");
        assertThat(test(grid).size()).isEqualTo(1);
    }

    // Scenario "Seasonal rate changed"
    @Test
    void changes_the_nightly_rate() {
        insertSeasonalRate(ctx, "Double", "Christmas", DECEMBER_20, JANUARY_6, "180.00");
        open();
        test(grid).select(0);
        assertThat(name.getValue()).isEqualTo("Christmas");
        nightlyRate.setValue(new BigDecimal("190.00"));
        save();

        assertThat(rows()).containsExactly("Double|Christmas|2030-12-20|2031-01-06|CHF 190.00");
    }

    // Scenario "Seasonal rate deleted"; delete needs a selected seasonal rate
    @Test
    void deletes_the_selected_seasonal_rate() {
        insertSeasonalRate(ctx, "Double", "Christmas", DECEMBER_20, JANUARY_6, "180.00");
        open();
        Button delete = $(Button.class).id("delete-seasonal-rate");
        assertThat(delete.isEnabled()).isFalse();

        test(grid).select(0);
        test(delete).click();

        assertThat(test(grid).size()).isZero();
    }

    // Scenario "Receptionist opens the seasonal rates page"
    @Test
    @WithUserDetails("reception")
    void receptionist_gets_access_denied() {
        navigate(SeasonalRateView.ROUTE, AccessDeniedView.class);

        assertThat($(H1.class).single().getText()).isEqualTo(AccessDeniedView.TEXT);
    }

    private void fill(String roomTypeName, String nameValue, LocalDate first, LocalDate last, String rate) {
        test(roomType).selectItem(roomTypeName);
        test(name).setValue(nameValue);
        test(firstNight).setValue(first);
        test(lastNight).setValue(last);
        nightlyRate.setValue(new BigDecimal(rate));
    }

    private void save() {
        test($(Button.class).id("save-seasonal-rate")).click();
    }

    private List<String> rows() {
        return IntStream.range(0, test(grid).size())
                .mapToObj(i -> String.join("|", IntStream.range(0, 5).mapToObj(c -> test(grid).getCellText(i, c))
                        .toList()))
                .toList();
    }
}
