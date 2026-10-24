package ch.martinelli.edu.hrs.reception.ui;

import ch.martinelli.edu.hrs.reception.Occupancy;
import ch.martinelli.edu.hrs.reception.RoomStatus;
import ch.martinelli.edu.hrs.reception.RoomStatusBoardService;
import ch.martinelli.edu.hrs.reservation.RoomTypeChoice;
import ch.martinelli.edu.hrs.staff.StaffRole;
import ch.martinelli.edu.hrs.staff.ui.StaffLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;

/**
 * UC-011 View Room Status Board: every active room with its occupancy and cleaning status on one screen. The rooms
 * are tiles that wrap, so the board needs no horizontal scrolling on a 768 px tablet (UC-011 BR-003, NFR-007). The
 * view changes no data.
 */
@Route(value = "room-status-board", layout = StaffLayout.class)
@PageTitle("Room Status Board")
// NFR-009: Only the receptionist can view the room status board.
@RolesAllowed(StaffRole.RECEPTIONIST_ROLE)
public class RoomStatusBoardView extends VerticalLayout {

    static final String NO_MATCH = "No room matches the selection.";

    private final RoomStatusBoardService service;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);

    private final Select<RoomTypeChoice> roomType = new Select<>();
    private final Select<Occupancy> occupancy = new Select<>();
    private final Select<Boolean> cleaningStatus = new Select<>();
    private final Div board = new Div();

    public RoomStatusBoardView(RoomStatusBoardService service) {
        this.service = service;
        add(new H1("Room Status Board"));
        add(createFilters());

        board.setId("board");
        board.setWidthFull();
        board.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fill, minmax(10rem, 1fr))")
                .set("gap", "var(--lumo-space-m)");
        add(board);
        showRooms();
    }

    private FormLayout createFilters() {
        // A1: the receptionist selects a room type, an occupancy or a cleaning status
        roomType.setLabel("Room type");
        roomType.setItems(service.roomTypes());
        roomType.setItemLabelGenerator(choice -> choice == null ? "All" : choice.name());
        roomType.setEmptySelectionAllowed(true);
        roomType.setEmptySelectionCaption("All");

        occupancy.setLabel("Occupancy");
        occupancy.setItems(Occupancy.values());
        occupancy.setItemLabelGenerator(value -> value == null ? "All" : occupancyText(value));
        occupancy.setEmptySelectionAllowed(true);
        occupancy.setEmptySelectionCaption("All");

        cleaningStatus.setLabel("Cleaning status");
        cleaningStatus.setItems(true, false);
        cleaningStatus.setItemLabelGenerator(value -> value == null ? "All" : cleaningText(value));
        cleaningStatus.setEmptySelectionAllowed(true);
        cleaningStatus.setEmptySelectionCaption("All");

        roomType.addValueChangeListener(_ -> showRooms());
        occupancy.addValueChangeListener(_ -> showRooms());
        cleaningStatus.addValueChangeListener(_ -> showRooms());

        // A2: the receptionist refreshes the board
        Button refresh = new Button("Refresh", _ -> showRooms());

        FormLayout filters = new FormLayout(roomType, occupancy, cleaningStatus, refresh);
        filters.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("30rem", 2),
                new FormLayout.ResponsiveStep("44rem", 4));
        filters.setWidthFull();
        return filters;
    }

    /**
     * Steps 2 and 3.
     */
    private void showRooms() {
        board.removeAll();
        RoomTypeChoice selectedRoomType = roomType.getValue();
        List<RoomStatus> rooms = service.rooms(selectedRoomType == null ? null : selectedRoomType.roomTypeId(),
                occupancy.getValue(), cleaningStatus.getValue());
        if (rooms.isEmpty()) {
            Paragraph noMatch = new Paragraph(NO_MATCH);
            noMatch.setId("no-room");
            board.add(noMatch);
            return;
        }
        rooms.forEach(room -> board.add(tile(room)));
    }

    private Div tile(RoomStatus room) {
        Div tile = new Div();
        tile.setId("room-" + room.roomNumber());
        tile.addClassNames("room-tile", LumoUtility.Border.ALL, LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.Padding.SMALL);

        H3 number = new H3(room.roomNumber());
        number.addClassNames(LumoUtility.Margin.NONE);
        tile.add(number, line("room-type", room.roomTypeName()));

        String occupancyText = occupancyText(room.occupancy());
        // UC-011 BR-001: An occupied room that is also blocked today is shown as Occupied with a marker.
        if (room.occupiedAndBlocked()) {
            occupancyText += " (blocked today)";
        }
        tile.add(line("occupancy", occupancyText), line("cleaning-status", cleaningText(room.clean())));

        // Step 3: guest and departure date of an occupied room, last blocked date and reason of a blocked room
        if (room.occupancy() == Occupancy.OCCUPIED) {
            tile.add(line("guest", room.guestName()),
                    line("departure-date", "Departs " + dateFormatter.format(room.departureDate())));
        } else if (room.occupancy() == Occupancy.BLOCKED) {
            tile.add(line("blocked-until", "Blocked until " + dateFormatter.format(room.blockedUntil())),
                    line("block-reason", room.blockReason() == null ? "" : room.blockReason()));
        }

        // UC-011 BR-002: Free and Clean rooms are highlighted as assignable.
        if (room.assignable()) {
            tile.addClassNames("assignable", LumoUtility.Background.SUCCESS_10, LumoUtility.BorderColor.SUCCESS);
            Span badge = line("assignable", "Assignable");
            badge.getElement().getThemeList().addAll(List.of("badge", "success"));
            tile.add(badge);
        } else {
            tile.addClassNames(LumoUtility.BorderColor.CONTRAST_10);
        }
        return tile;
    }

    private static Span line(String className, String text) {
        Span span = new Span(text);
        span.addClassNames(className, LumoUtility.Display.BLOCK, LumoUtility.FontSize.SMALL);
        return span;
    }

    static String occupancyText(Occupancy occupancy) {
        return switch (occupancy) {
            case FREE -> "Free";
            case OCCUPIED -> "Occupied";
            case BLOCKED -> "Blocked";
        };
    }

    static String cleaningText(boolean clean) {
        return clean ? "Clean" : "Dirty";
    }
}
