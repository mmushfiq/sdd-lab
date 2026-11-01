package ch.martinelli.edu.hrs.report.ui;

import ch.martinelli.edu.hrs.core.ui.AccessDeniedView;
import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.select.Select;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.IntStream;

import static ch.martinelli.edu.hrs.reservation.ReservationTestData.insert;
import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@WithUserDetails("manager")
class ReportViewTest extends HrsBrowserlessTest {

    // Today is 10 March 2031
    @TestBean
    Clock clock;

    static Clock clock() {
        return Clock.fixed(Instant.parse("2031-03-10T08:00:00Z"), ZoneId.of("Europe/Zurich"));
    }

    @Autowired
    DSLContext ctx;

    // Scenarios "Report for the current year", "Occupancy computed" and "Revenue from saved rates"
    @Test
    void current_year_preselected_with_twelve_rows_and_total() {
        // April 2031: 6 rooms x 10 nights = 60 nights = 20.0 %; 3 x 140.00 + 2 x 220.00 = 860.00 on top
        String[] rooms = {"101", "102", "103", "104", "105", "106"};
        for (int i = 0; i < rooms.length; i++) {
            insert(ctx, "OCC0000" + i, rooms[i], LocalDate.of(2031, 4, 1), LocalDate.of(2031, 4, 11),
                    "Guest " + i, "CHECKED_OUT");
        }
        insert(ctx, "REV00001", "107", LocalDate.of(2031, 5, 1), LocalDate.of(2031, 5, 4), "Anna", "CHECKED_OUT");
        insert(ctx, "REV00002", "109", LocalDate.of(2031, 5, 10), LocalDate.of(2031, 5, 12), "Ben", "CHECKED_OUT",
                "220.00", "220.00");

        navigate(ReportView.class);

        assertThat(year().getValue()).isEqualTo(2031);
        assertThat(rows()).hasSize(12);
        assertThat(rows().getFirst()).isEqualTo("January|0|0.0 %|CHF 0.00");
        assertThat(rows().get(3)).isEqualTo("April|60|20.0 %|CHF 8400.00");
        assertThat(rows().get(4)).isEqualTo("May|5|1.6 %|CHF 860.00");
        assertThat(footer()).isEqualTo("Total 2031|65|1.8 %|CHF 9260.00");
    }

    // Scenarios "Other year chosen" and "Year without stays"
    @Test
    void choosing_another_year_reloads_the_figures() {
        insert(ctx, "Y2030001", "104", LocalDate.of(2030, 7, 1), LocalDate.of(2030, 7, 3), "Anna", "CHECKED_OUT");
        insert(ctx, "Y2031001", "104", LocalDate.of(2031, 1, 5), LocalDate.of(2031, 1, 6), "Ben", "CHECKED_OUT");
        navigate(ReportView.class);
        assertThat(year().getListDataView().getItems().toList()).containsExactly(2030, 2031, 2032);

        year().setValue(2030);

        assertThat(rows().get(6)).isEqualTo("July|2|0.6 %|CHF 280.00");
        assertThat(rows().getFirst()).isEqualTo("January|0|0.0 %|CHF 0.00");
        assertThat(footer()).isEqualTo("Total 2030|2|0.1 %|CHF 280.00");

        year().setValue(2032);

        assertThat(rows()).hasSize(12).allMatch(r -> r.endsWith("|0|0.0 %|CHF 0.00"));
        assertThat(footer()).isEqualTo("Total 2032|0|0.0 %|CHF 0.00");
    }

    // Scenario "Receptionist opens the report page"
    @Test
    @WithUserDetails("reception")
    void receptionist_gets_access_denied() {
        navigate(ReportView.ROUTE, AccessDeniedView.class);

        assertThat($(H1.class).single().getText()).isEqualTo(AccessDeniedView.TEXT);
        assertThat($(Grid.class).exists()).isFalse();
    }

    @SuppressWarnings("unchecked")
    private Select<Integer> year() {
        return $(Select.class).id("report-year");
    }

    @SuppressWarnings("unchecked")
    private Grid<Object> grid() {
        return $(Grid.class).id("report");
    }

    private List<String> rows() {
        var tester = test(grid());
        return IntStream.range(0, tester.size())
                .mapToObj(row -> String.join("|", IntStream.range(0, 4)
                        .mapToObj(col -> tester.getCellText(row, col)).toList()))
                .toList();
    }

    private String footer() {
        var footer = grid().getFooterRows().getFirst();
        return String.join("|", grid().getColumns().stream().map(c -> footer.getCell(c).getText()).toList());
    }
}
