package ch.martinelli.edu.hrs.housekeeping.ui;

import ch.martinelli.edu.hrs.core.ui.StaffLayout;
import ch.martinelli.edu.hrs.housekeeping.HousekeepingService;
import ch.martinelli.edu.hrs.housekeeping.HousekeepingService.DirtyRoom;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AccessDeniedErrorRouter;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;

/**
 * Cleaning list for a tablet: one card per dirty room, stacked vertically, with a large "Cleaned" button.
 */
@Route(value = CleaningListView.ROUTE, layout = StaffLayout.class)
@PageTitle("Cleaning")
@RolesAllowed("HOUSEKEEPING")
@AccessDeniedErrorRouter
public class CleaningListView extends VerticalLayout {

    public static final String ROUTE = "cleaning";
    public static final String ALL_CLEAN = "All rooms are clean";

    private final HousekeepingService housekeepingService;
    private final VerticalLayout rooms = new VerticalLayout();

    public CleaningListView(HousekeepingService housekeepingService) {
        this.housekeepingService = housekeepingService;

        Button refresh = new Button("Refresh", _ -> refresh());
        refresh.setId("refresh-cleaning-list");
        refresh.addThemeVariants(ButtonVariant.LUMO_LARGE);
        rooms.setId("cleaning-list");
        rooms.setPadding(false);

        add(new H2("Cleaning"), refresh, rooms);
        refresh();
    }

    private void refresh() {
        rooms.removeAll();
        List<DirtyRoom> dirtyRooms = housekeepingService.findDirtyRooms();
        if (dirtyRooms.isEmpty()) {
            Paragraph allClean = new Paragraph(ALL_CLEAN);
            allClean.setId("all-rooms-clean");
            rooms.add(allClean);
        }
        dirtyRooms.forEach(room -> rooms.add(card(room)));
    }

    private HorizontalLayout card(DirtyRoom room) {
        H3 number = new H3(room.roomNumber());
        number.getStyle().set("margin", "0");
        VerticalLayout text = new VerticalLayout(number, new Span(room.roomTypeName()));
        text.setPadding(false);
        text.setSpacing(false);

        Button cleaned = new Button("Cleaned", _ -> {
            housekeepingService.markCleaned(room.roomId());
            refresh();
        });
        cleaned.setId("cleaned-" + room.roomNumber());
        cleaned.addThemeVariants(ButtonVariant.LUMO_LARGE, ButtonVariant.LUMO_PRIMARY);
        // Touch target of at least 44 x 44 px
        cleaned.getStyle().set("min-height", "44px").set("min-width", "44px");

        HorizontalLayout card = new HorizontalLayout(text, cleaned);
        card.setId("room-" + room.roomNumber());
        card.setWidthFull();
        card.setAlignItems(FlexComponent.Alignment.CENTER);
        card.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        card.addClassNames("border", "rounded-m", "p-m", "box-border");
        return card;
    }
}
