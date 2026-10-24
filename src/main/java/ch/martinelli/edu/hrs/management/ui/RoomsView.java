package ch.martinelli.edu.hrs.management.ui;

import ch.martinelli.edu.hrs.management.ReservationsWouldLoseRoomException;
import ch.martinelli.edu.hrs.management.RoomData;
import ch.martinelli.edu.hrs.management.RoomNumberUsedException;
import ch.martinelli.edu.hrs.management.RoomOccupiedException;
import ch.martinelli.edu.hrs.management.RoomOverview;
import ch.martinelli.edu.hrs.management.RoomService;
import ch.martinelli.edu.hrs.management.RoomTypeOverview;
import ch.martinelli.edu.hrs.staff.StaffRole;
import ch.martinelli.edu.hrs.staff.ui.StaffLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.stream.Collectors;

/**
 * UC-017 Maintain Rooms: the manager creates and edits rooms.
 * <p>
 * Nothing is stored before the manager saves, so leaving the room discards the entered values (A5).
 */
@Route(value = "rooms", layout = StaffLayout.class)
@PageTitle("Rooms")
// NFR-009: Only the manager can maintain rooms.
@RolesAllowed(StaffRole.MANAGER_ROLE)
public class RoomsView extends VerticalLayout {

    static final String NUMBER_USED = "The room number is already used by another room.";
    static final String OCCUPIED = "An occupied room can be deactivated or moved to another room type only after "
            + "check-out.";
    static final String WOULD_LOSE_ROOM = "The %s would have more reservations than rooms on these nights: %s.";
    static final String SAVED = "Room %s is saved.";

    private final RoomService service;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
    private final Grid<RoomOverview> grid = new Grid<>();
    private final Div message = new Div();

    private final VerticalLayout editor = new VerticalLayout();
    private final H2 editorTitle = new H2();
    private final TextField roomNumber = new TextField("Room number");
    private final Select<RoomTypeOverview> roomType = new Select<>();
    private final Checkbox active = new Checkbox("Active");
    private final Binder<RoomInput> binder = new Binder<>();

    private Long editedRoomId;
    private RoomInput input;

    public RoomsView(RoomService service) {
        this.service = service;
        add(new H1("Rooms"));

        // Step 3: create a new room
        Button newButton = new Button("New room", _ -> edit(null));
        newButton.addThemeVariants(ButtonVariant.PRIMARY);
        add(newButton);

        message.setWidthFull();
        message.getElement().setAttribute("aria-live", "polite");
        add(message);

        createGrid();
        add(grid);
        createEditor();
        add(editor);
        refreshGrid();
    }

    /**
     * Step 2: room number, room type, cleaning status and whether active, by room number.
     */
    private void createGrid() {
        grid.setId("rooms");
        grid.addColumn(RoomOverview::roomNumber).setHeader("Room number").setAutoWidth(true);
        grid.addColumn(RoomOverview::roomTypeName).setHeader("Room type").setAutoWidth(true);
        grid.addColumn(room -> room.clean() ? "Clean" : "Dirty").setHeader("Cleaning status").setAutoWidth(true);
        grid.addColumn(room -> room.active() ? "Active" : "Inactive").setHeader("Status").setAutoWidth(true);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        // Step 3: select an existing room
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                edit(event.getValue());
            }
        });
    }

    private void createEditor() {
        roomNumber.setMaxLength(RoomService.MAX_ROOM_NUMBER_LENGTH);
        roomNumber.setRequiredIndicatorVisible(true);
        roomType.setLabel("Room type");
        roomType.setItemLabelGenerator(type -> type == null ? "" : type.name());
        roomType.setRequiredIndicatorVisible(true);

        // UC-017 A1: The room number is required and at most 10 characters.
        binder.forField(roomNumber)
                .asRequired("Enter the room number")
                .withValidator(value -> !value.isBlank(), "Enter the room number")
                .withValidator(value -> value.strip().length() <= RoomService.MAX_ROOM_NUMBER_LENGTH,
                        "The room number can have at most " + RoomService.MAX_ROOM_NUMBER_LENGTH + " characters")
                .bind(RoomInput::getRoomNumber, RoomInput::setRoomNumber);
        // UC-017 A1: A room type is selected.
        binder.forField(roomType)
                .asRequired("Select a room type")
                .bind(RoomInput::getRoomType, RoomInput::setRoomType);
        binder.forField(active).bind(RoomInput::isActive, RoomInput::setActive);

        FormLayout form = new FormLayout(roomNumber, roomType, active);
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("30rem", 3));

        // Step 5
        Button saveButton = new Button("Save", _ -> save());
        saveButton.addThemeVariants(ButtonVariant.PRIMARY);
        // A5: the manager leaves the room without saving
        Button discardButton = new Button("Discard", _ -> closeEditor());
        discardButton.addThemeVariants(ButtonVariant.TERTIARY);

        editor.setPadding(false);
        editor.add(editorTitle, form, new HorizontalLayout(saveButton, discardButton));
        editor.setVisible(false);
    }

    /**
     * Steps 3 and 4.
     */
    private void edit(RoomOverview room) {
        message.removeAll();
        List<RoomTypeOverview> roomTypes = service.roomTypes();
        roomType.setItems(roomTypes);
        editedRoomId = room == null ? null : room.id();
        input = new RoomInput();
        if (room == null) {
            input.setRoomNumber("");
            input.setActive(true);
            editorTitle.setText("New room");
        } else {
            input.setRoomNumber(room.roomNumber());
            input.setActive(room.active());
            roomTypes.stream().filter(type -> type.id().equals(room.roomTypeId())).findFirst()
                    .ifPresent(input::setRoomType);
            editorTitle.setText("Room " + room.roomNumber());
        }
        binder.setBean(input);
        editor.setVisible(true);
        roomNumber.focus();
    }

    /**
     * Steps 5 to 8, A1 to A4.
     */
    private void save() {
        message.removeAll();
        // A1: invalid values are marked; the entered values are kept
        if (!binder.validate().isOk()) {
            return;
        }
        RoomData data = new RoomData(input.getRoomNumber(), input.getRoomType().id(), input.isActive());
        try {
            service.save(editedRoomId, data);
        } catch (RoomNumberUsedException _) {
            // A2
            roomNumber.setErrorMessage(NUMBER_USED);
            roomNumber.setInvalid(true);
            showMessage(NUMBER_USED, true);
            return;
        } catch (ReservationsWouldLoseRoomException e) {
            // A3: the affected nights and room type
            String nights = e.nights().stream().map(dateFormatter::format).collect(Collectors.joining(", "));
            showMessage(WOULD_LOSE_ROOM.formatted(e.roomTypeName(), nights), true);
            return;
        } catch (RoomOccupiedException _) {
            // A4
            showMessage(OCCUPIED, true);
            return;
        }
        // Step 8: the updated list
        String savedNumber = data.roomNumber().strip();
        closeEditor();
        showMessage(SAVED.formatted(savedNumber), false);
    }

    private void closeEditor() {
        editor.setVisible(false);
        editedRoomId = null;
        binder.setBean(null);
        refreshGrid();
    }

    private void refreshGrid() {
        grid.setItems(service.rooms());
    }

    private void showMessage(String text, boolean error) {
        Paragraph paragraph = new Paragraph(text);
        paragraph.setId("room-message");
        if (error) {
            paragraph.getElement().setAttribute("role", "alert");
            paragraph.addClassNames(LumoUtility.TextColor.ERROR);
        }
        message.removeAll();
        message.add(paragraph);
    }

    private static class RoomInput {
        private String roomNumber;
        private RoomTypeOverview roomType;
        private boolean active;

        String getRoomNumber() {
            return roomNumber;
        }

        void setRoomNumber(String roomNumber) {
            this.roomNumber = roomNumber;
        }

        RoomTypeOverview getRoomType() {
            return roomType;
        }

        void setRoomType(RoomTypeOverview roomType) {
            this.roomType = roomType;
        }

        boolean isActive() {
            return active;
        }

        void setActive(boolean active) {
            this.active = active;
        }
    }
}
