package ch.martinelli.edu.hrs.housekeeping.ui;

import ch.martinelli.edu.hrs.housekeeping.CleaningListEntry;
import ch.martinelli.edu.hrs.housekeeping.CleaningListService;
import ch.martinelli.edu.hrs.housekeeping.MarkRoomCleanService;
import ch.martinelli.edu.hrs.housekeeping.RoomAlreadyCleanException;
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
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;

/**
 * UC-013 View Cleaning List: the dirty rooms with today's departures first; the start screen of the housekeeper after
 * login (UC-022 step 5). Four short columns keep the list usable without horizontal scrolling on a 768 px tablet
 * (UC-013 BR-003, NFR-007).
 * <p>
 * UC-014 Mark Room Clean: the housekeeper marks the selected room clean.
 */
@Route(value = "cleaning-list", layout = StaffLayout.class)
@PageTitle("Cleaning List")
// UC-022 BR-003: Only the housekeeper can reach UC-013.
@RolesAllowed(StaffRole.HOUSEKEEPER_ROLE)
public class CleaningListView extends VerticalLayout {

    static final String ALL_CLEAN = "All rooms are clean.";
    static final String MARKED_CLEAN = "Room %s is clean.";
    static final String ALREADY_CLEAN = "Room %s is already clean.";

    private final CleaningListService cleaningListService;
    private final MarkRoomCleanService markRoomCleanService;
    private final Div message = new Div();
    private final Div list = new Div();
    private final Div selectedRoom = new Div();

    public CleaningListView(CleaningListService cleaningListService, MarkRoomCleanService markRoomCleanService) {
        this.cleaningListService = cleaningListService;
        this.markRoomCleanService = markRoomCleanService;
        add(new H1("Cleaning List"));
        message.setWidthFull();
        message.getElement().setAttribute("aria-live", "polite");
        list.setWidthFull();
        selectedRoom.setWidthFull();
        add(message, list, selectedRoom);
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
        // Step 3; UC-014 A2: the housekeeper selects another room
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
        // UC-014 step 2
        Button markClean = new Button("Mark clean", _ -> markClean(room));
        markClean.addThemeVariants(ButtonVariant.PRIMARY);
        selectedRoom.add(new H2("Room " + room.roomNumber()), details, markClean);
    }

    /**
     * UC-014 steps 3 to 5, A1.
     */
    private void markClean(CleaningListEntry room) {
        message.removeAll();
        try {
            markRoomCleanService.markClean(room.roomId());
            // UC-014 step 5: the room is removed from the cleaning list
            message.add(paragraph(MARKED_CLEAN.formatted(room.roomNumber()), false));
        } catch (RoomAlreadyCleanException _) {
            // UC-014 A1: informed, and the cleaning list is refreshed
            message.add(paragraph(ALREADY_CLEAN.formatted(room.roomNumber()), true));
        }
        showList();
    }

    private static Paragraph paragraph(String text, boolean error) {
        Paragraph paragraph = new Paragraph(text);
        paragraph.setId("cleaning-message");
        if (error) {
            paragraph.getElement().setAttribute("role", "alert");
            paragraph.addClassNames(LumoUtility.TextColor.ERROR);
        }
        return paragraph;
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
