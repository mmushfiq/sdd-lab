package ch.martinelli.edu.hrs.housekeeping.ui;

import ch.martinelli.edu.hrs.core.ui.AccessDeniedView;
import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import ch.martinelli.edu.hrs.housekeeping.HousekeepingService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@WithUserDetails("housekeeping")
class CleaningListViewTest extends HrsBrowserlessTest {

    @Autowired
    DSLContext ctx;

    // Scenario "Dirty rooms listed"
    @Test
    void lists_dirty_rooms_sorted_by_number() {
        setStatus(HousekeepingService.DIRTY, "104", "102");
        navigate(CleaningListView.class);

        assertThat(cards()).containsExactly("102|Single", "104|Double");
    }

    // Scenario "Room marked as cleaned"
    @Test
    void cleaned_removes_room_104() {
        setStatus(HousekeepingService.DIRTY, "104", "102");
        navigate(CleaningListView.class);

        test($(Button.class).id("cleaned-104")).click();

        assertThat(cards()).containsExactly("102|Single");
        assertThat(status("104")).isEqualTo(HousekeepingService.CLEAN);
    }

    // Scenario "No dirty rooms"
    @Test
    void no_dirty_rooms_shows_message() {
        navigate(CleaningListView.class);

        assertThat(cards()).isEmpty();
        assertThat($(Paragraph.class).id("all-rooms-clean").getText()).isEqualTo("All rooms are clean");
    }

    // Scenario "Room already marked by someone else"
    @Test
    void room_cleaned_by_someone_else_stays_clean_and_leaves_list() {
        setStatus(HousekeepingService.DIRTY, "104");
        navigate(CleaningListView.class);
        setStatus(HousekeepingService.CLEAN, "104");

        test($(Button.class).id("cleaned-104")).click();

        assertThat(status("104")).isEqualTo(HousekeepingService.CLEAN);
        assertThat(cards()).isEmpty();
        assertThat($(Paragraph.class).id("all-rooms-clean").getText()).isEqualTo("All rooms are clean");
    }

    // Refresh shows rooms that became dirty after the list was opened
    @Test
    void refresh_reloads_the_list() {
        navigate(CleaningListView.class);
        setStatus(HousekeepingService.DIRTY, "104");

        test($(Button.class).id("refresh-cleaning-list")).click();

        assertThat(cards()).containsExactly("104|Double");
    }

    // Scenario "Manager opens the cleaning list"
    @Test
    @WithUserDetails("manager")
    void manager_gets_access_denied() {
        navigate(CleaningListView.ROUTE, AccessDeniedView.class);

        assertThat($(H1.class).single().getText()).isEqualTo(AccessDeniedView.TEXT);
    }

    private void setStatus(String status, String... roomNumbers) {
        ctx.update(ROOM).set(ROOM.CLEANING_STATUS, status).where(ROOM.ROOM_NUMBER.in(roomNumbers)).execute();
    }

    private String status(String number) {
        return ctx.select(ROOM.CLEANING_STATUS).from(ROOM).where(ROOM.ROOM_NUMBER.eq(number))
                .fetchSingle(ROOM.CLEANING_STATUS);
    }

    private List<String> cards() {
        return $(HorizontalLayout.class).all().stream()
                .filter(c -> c.getId().orElse("").startsWith("room-"))
                .map(c -> $(H3.class, c).single().getText() + "|" + $(Span.class, c).single().getText())
                .toList();
    }
}
