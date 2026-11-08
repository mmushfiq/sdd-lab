package ch.martinelli.edu.hrs.housekeeping;

import ch.martinelli.edu.hrs.core.ui.MainLayout;
import ch.martinelli.edu.hrs.db.tables.records.RoomRecord;
import ch.martinelli.edu.hrs.room.RoomService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import jakarta.annotation.security.RolesAllowed;

/**
 * Housekeeping's cleaning list on a tablet: dirty rooms only, no guest data (FR-020, FR-024). The list polls
 * every 30 seconds so a check-out shows up without any step by reception (research R7, SC-005).
 */
@Route(value = "housekeeping", layout = MainLayout.class)
@PageTitle("Cleaning list | Hotel Reservation System")
@Menu(title = "Cleaning list", order = 30)
@RolesAllowed("HOUSEKEEPING")
public class CleaningListView extends VerticalLayout {

    static final int POLL_MILLIS = 30_000;

    private final transient RoomService roomService;
    private final Grid<RoomRecord> grid = new Grid<>();
    private Registration pollRegistration;

    public CleaningListView(RoomService roomService) {
        this.roomService = roomService;
        setSizeFull();

        grid.addColumn(RoomRecord::getRoomNumber).setHeader("Room");
        grid.addColumn(_ -> "Dirty").setHeader("Status");
        grid.addThemeName("row-stripes");
        grid.getStyle().set("font-size", "var(--lumo-font-size-xl)");

        var markCleaned = new Button("Mark cleaned", _ -> {
            roomService.markCleaned(grid.asSingleSelect().getValue().getId());
            refresh();
        });
        markCleaned.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        markCleaned.setEnabled(false);
        grid.asSingleSelect().addValueChangeListener(e -> markCleaned.setEnabled(e.getValue() != null));
        var refresh = new Button("Refresh", _ -> refresh());
        refresh.addThemeVariants(ButtonVariant.LUMO_LARGE);

        add(new H2("Dirty rooms"), new HorizontalLayout(markCleaned, refresh), grid);
    }

    @Override
    protected void onAttach(AttachEvent event) {
        refresh();
        var ui = event.getUI();
        ui.setPollInterval(POLL_MILLIS);
        pollRegistration = ui.addPollListener(_ -> refresh());
    }

    @Override
    protected void onDetach(DetachEvent event) {
        event.getUI().setPollInterval(-1);
        pollRegistration.remove();
    }

    private void refresh() {
        grid.setItems(roomService.findDirtyRooms());
    }
}
