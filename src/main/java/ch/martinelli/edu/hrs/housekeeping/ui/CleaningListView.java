package ch.martinelli.edu.hrs.housekeeping.ui;

import ch.martinelli.edu.hrs.housekeeping.CleaningListEntry;
import ch.martinelli.edu.hrs.housekeeping.CleaningListService;
import ch.martinelli.edu.hrs.staff.StaffRole;
import ch.martinelli.edu.hrs.staff.ui.StaffLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;

/**
 * UC-013 View Cleaning List: the dirty rooms with today's departures first; the start screen of the housekeeper after
 * login (UC-022 step 5). Four short columns keep the list usable without horizontal scrolling on a 768 px tablet
 * (UC-013 BR-003, NFR-007).
 */
@Route(value = "cleaning-list", layout = StaffLayout.class)
@PageTitle("Cleaning List")
// UC-022 BR-003: Only the housekeeper can reach UC-013.
@RolesAllowed(StaffRole.HOUSEKEEPER_ROLE)
public class CleaningListView extends VerticalLayout {

    static final String ALL_CLEAN = "All rooms are clean.";

    private final CleaningListService cleaningListService;
    private final Div list = new Div();
    private final Div selectedRoom = new Div();

    public CleaningListView(CleaningListService cleaningListService) {
        this.cleaningListService = cleaningListService;
        add(new H1("Cleaning List"));
        list.setWidthFull();
        selectedRoom.setWidthFull();
        add(list, selectedRoom);
        showList();
    }

    /**
     * Step 2, A1.
     */
    private void showList() {
        list.removeAll();
        selectedRoom.removeAll();
        List<CleaningListEntry> rooms = cleaningListService.cleaningList();
        if (rooms.isEmpty()) {
            // A1
            Paragraph allClean = new Paragraph(ALL_CLEAN);
            allClean.setId("all-clean");
            list.add(allClean);
            return;
        }
        Grid<CleaningListEntry> grid = new Grid<>();
        grid.setId("cleaning-list");
        grid.addColumn(CleaningListEntry::roomNumber).setHeader("Room").setAutoWidth(true);
        grid.addColumn(CleaningListEntry::roomTypeName).setHeader("Room type").setAutoWidth(true);
        grid.addColumn(room -> occupancy(room)).setHeader("Occupancy").setAutoWidth(true);
        grid.addColumn(CleaningListView::departure).setHeader("Departure").setAutoWidth(true);
        grid.setItems(rooms);
        grid.setAllRowsVisible(true);
        grid.setWidthFull();
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_WRAP_CELL_CONTENT);
        // Step 3
        grid.asSingleSelect().addValueChangeListener(event -> showRoom(event.getValue()));
        list.add(grid);
    }

    /**
     * Step 4: the room with the option to mark it clean (UC-014).
     */
    private void showRoom(CleaningListEntry room) {
        selectedRoom.removeAll();
        if (room == null) {
            return;
        }
        Paragraph details = new Paragraph(room.roomTypeName() + ", " + occupancy(room)
                + (departure(room).isEmpty() ? "" : ", " + departure(room)));
        details.setId("room-details");
        Button markClean = new Button("Mark clean");
        markClean.addThemeVariants(ButtonVariant.PRIMARY);
        markClean.setEnabled(false);
        selectedRoom.add(new H2("Room " + room.roomNumber()), details, markClean);
    }

    private static String occupancy(CleaningListEntry room) {
        return room.occupied() ? "Occupied" : "Free";
    }

    private static String departure(CleaningListEntry room) {
        if (room.checkedOutToday() && !room.occupied()) {
            return "Checked out today";
        }
        if (room.departsToday()) {
            return "Departs today";
        }
        return "";
    }
}
