package ch.martinelli.edu.hrs.room;

import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import ch.martinelli.edu.hrs.db.tables.records.SeasonalRateRecord;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import static ch.martinelli.edu.hrs.room.RoomTypeServiceTest.roomType;
import static ch.martinelli.edu.hrs.room.SeasonalRateServiceTest.rate;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 003 US1: route manager/seasonal-rates (contracts/ui-changes.md).
 */
@Transactional
@WithMockUser(roles = "MANAGER")
class SeasonalRateViewTest extends HrsBrowserlessTest {

    @Autowired
    private RoomTypeService roomTypeService;

    @Autowired
    private SeasonalRateService seasonalRateService;

    private long typeId;

    @BeforeEach
    void createRoomType() {
        typeId = roomTypeService.save(roomType("Test View Double", 2, "100.00")).getId();
    }

    @Test
    void gridShowsTheSeasonalRates() {
        seasonalRateService.save(rate(typeId, "Summer", "2027-07-01", "2027-08-31", "150.00"));
        navigate(SeasonalRateView.class);

        assertThat(grid().getColumns()).extracting(Grid.Column::getHeaderText)
                .containsExactly("Room type", "Name", "Start date", "End date", "Nightly rate");
        var row = rowOf("Summer");
        var grid = test(grid());
        assertThat(IntStream.range(0, 5).mapToObj(c -> grid.getCellText(row, c)).toList())
                .containsExactly("Test View Double", "Summer", "2027-07-01", "2027-08-31", "150.00");
    }

    @Test
    void newSeasonalRateIsSavedAndListed() {
        navigate(SeasonalRateView.class);

        fillForm("Christmas", LocalDate.parse("2027-12-20"), LocalDate.parse("2028-01-02"), "180.00");
        test($(Button.class).withText("Save").single()).click();

        assertThat(names()).contains("Christmas");
        assertThat($(TextField.class).withLabel("Name").single().getValue()).isEmpty();
    }

    @Test
    void selectedRateIsChanged() {
        seasonalRateService.save(rate(typeId, "Summer", "2027-07-01", "2027-08-31", "150.00"));
        navigate(SeasonalRateView.class);

        test(grid()).select(rowOf("Summer"));
        test($(BigDecimalField.class).withLabel("Nightly rate").single()).setValue(new BigDecimal("160.00"));
        test($(Button.class).withText("Save").single()).click();

        assertThat(seasonalRateService.findAll()).filteredOn(r -> r.getRoomTypeId() == typeId).singleElement()
                .satisfies(r -> assertThat(r.getNightlyRate()).isEqualByComparingTo("160.00"));
    }

    @Test
    void deleteIsEnabledOnlyWithASelection() {
        seasonalRateService.save(rate(typeId, "Summer", "2027-07-01", "2027-08-31", "150.00"));
        navigate(SeasonalRateView.class);
        var delete = $(Button.class).withText("Delete").single();
        assertThat(delete.isEnabled()).isFalse();

        test(grid()).select(rowOf("Summer"));
        assertThat(delete.isEnabled()).isTrue();
        test(delete).click();

        assertThat(names()).doesNotContain("Summer");
        assertThat(delete.isEnabled()).isFalse();
    }

    @Test
    void newSeasonalRateButtonClearsTheForm() {
        seasonalRateService.save(rate(typeId, "Summer", "2027-07-01", "2027-08-31", "150.00"));
        navigate(SeasonalRateView.class);

        test(grid()).select(rowOf("Summer"));
        test($(Button.class).withText("New seasonal rate").single()).click();

        assertThat($(TextField.class).withLabel("Name").single().getValue()).isEmpty();
        assertThat($(Button.class).withText("Delete").single().isEnabled()).isFalse();
    }

    @Test
    void overlapShowsTheServiceMessage() {
        seasonalRateService.save(rate(typeId, "Summer", "2027-07-01", "2027-08-31", "150.00"));
        navigate(SeasonalRateView.class);

        fillForm("Late summer", LocalDate.parse("2027-08-31"), LocalDate.parse("2027-09-15"), "130.00");
        test($(Button.class).withText("Save").single()).click();

        assertThat(test($(Notification.class).last()).getText())
                .isEqualTo("Overlaps the seasonal rate \"Summer\" (1 July 2027 – 31 August 2027).");
        assertThat(names()).doesNotContain("Late summer");
    }

    @Test
    void missingNameIsNotSaved() {
        navigate(SeasonalRateView.class);

        fillForm("", LocalDate.parse("2027-07-01"), LocalDate.parse("2027-08-31"), "150.00");
        test($(Button.class).withText("Save").single()).click();

        assertThat(seasonalRateService.findAll()).noneMatch(r -> r.getRoomTypeId() == typeId);
    }

    @Test
    @WithMockUser(roles = "RECEPTIONIST")
    void receptionistIsDenied() {
        assertThat(opens(SeasonalRateView.class)).isFalse();
    }

    @Test
    @WithMockUser(roles = "HOUSEKEEPING")
    void housekeepingIsDenied() {
        assertThat(opens(SeasonalRateView.class)).isFalse();
    }

    private void fillForm(String name, LocalDate start, LocalDate end, String nightlyRate) {
        test($(ComboBox.class).withLabel("Room type").single()).selectItem("Test View Double");
        test($(TextField.class).withLabel("Name").single()).setValue(name);
        test($(DatePicker.class).withLabel("Start date").single()).setValue(start);
        test($(DatePicker.class).withLabel("End date").single()).setValue(end);
        test($(BigDecimalField.class).withLabel("Nightly rate").single()).setValue(new BigDecimal(nightlyRate));
    }

    @SuppressWarnings("unchecked")
    private Grid<SeasonalRateRecord> grid() {
        return $(Grid.class).id("seasonal-rates");
    }

    private List<String> names() {
        var grid = test(grid());
        return IntStream.range(0, grid.size()).mapToObj(i -> grid.getRow(i).getName()).toList();
    }

    private int rowOf(String name) {
        var index = names().indexOf(name);
        assertThat(index).as("row " + name).isNotNegative();
        return index;
    }
}
