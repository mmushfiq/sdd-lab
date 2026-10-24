package ch.martinelli.edu.hrs.management.ui;

import ch.martinelli.edu.hrs.management.BlockableRoom;
import ch.martinelli.edu.hrs.management.ReservationsWouldLoseRoomException;
import ch.martinelli.edu.hrs.management.RoomBlockData;
import ch.martinelli.edu.hrs.management.RoomBlockOverlapException;
import ch.martinelli.edu.hrs.management.RoomBlockOverview;
import ch.martinelli.edu.hrs.management.RoomBlockService;
import ch.martinelli.edu.hrs.management.RoomOccupiedDuringBlockException;
import ch.martinelli.edu.hrs.staff.StaffRole;
import ch.martinelli.edu.hrs.staff.ui.StaffLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
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
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.stream.Collectors;

/**
 * UC-018 Block Room: the manager blocks a room for a date range.
 * <p>
 * Nothing is stored before the manager saves, so leaving the block discards the entered values (A5).
 */
@Route(value = "room-blocks", layout = StaffLayout.class)
@PageTitle("Room blocks")
// NFR-009: Only the manager can block rooms.
@RolesAllowed(StaffRole.MANAGER_ROLE)
public class RoomBlocksView extends VerticalLayout {

    static final String OVERLAP = "Room %s is already blocked from %s to %s.";
    static final String OCCUPIED = "Room %s is occupied by reservation %s during the block.";
    static final String WOULD_LOSE_ROOM = "The %s would have more reservations than rooms on these nights: %s.";
    static final String SAVED = "Room %s is blocked from %s to %s.";

    private final RoomBlockService service;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
    private final Grid<RoomBlockOverview> grid = new Grid<>();
    private final Div message = new Div();

    private final VerticalLayout editor = new VerticalLayout();
    private final Select<BlockableRoom> room = new Select<>();
    private final DatePicker startDate = new DatePicker("First blocked date");
    private final DatePicker endDate = new DatePicker("Last blocked date");
    private final TextArea reason = new TextArea("Reason");
    private final Binder<RoomBlockInput> binder = new Binder<>();

    private RoomBlockInput input;

    public RoomBlocksView(RoomBlockService service) {
        this.service = service;
        add(new H1("Room blocks"));

        // Step 3
        Button newButton = new Button("New room block", _ -> edit());
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
     * Step 2: room number, first blocked date, last blocked date and reason.
     */
    private void createGrid() {
        grid.setId("room-blocks");
        grid.addColumn(RoomBlockOverview::roomNumber).setHeader("Room number").setAutoWidth(true);
        grid.addColumn(block -> dateFormatter.format(block.startDate())).setHeader("First blocked date")
                .setAutoWidth(true);
        grid.addColumn(block -> dateFormatter.format(block.endDate())).setHeader("Last blocked date")
                .setAutoWidth(true);
        grid.addColumn(block -> block.reason() == null ? "" : block.reason()).setHeader("Reason")
                .setFlexGrow(1);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
    }

    private void createEditor() {
        room.setLabel("Room");
        room.setItemLabelGenerator(r -> r == null ? "" : r.roomNumber() + " (" + r.roomTypeName() + ")");
        room.setRequiredIndicatorVisible(true);
        startDate.setRequiredIndicatorVisible(true);
        startDate.setHelperText("Today or later");
        endDate.setRequiredIndicatorVisible(true);
        endDate.setHelperText("On or after the first blocked date");
        reason.setMaxLength(RoomBlockService.MAX_REASON_LENGTH);

        binder.forField(room)
                .asRequired("Select a room")
                .bind(RoomBlockInput::getRoom, RoomBlockInput::setRoom);
        // UC-018 BR-001: The first blocked date is today or later.
        binder.forField(startDate)
                .asRequired("Enter the first blocked date")
                .withValidator(date -> !date.isBefore(service.today()), "The first blocked date must be today or later")
                .bind(RoomBlockInput::getStartDate, RoomBlockInput::setStartDate);
        // UC-018 BR-001: The last blocked date is on or after the first blocked date.
        binder.forField(endDate)
                .asRequired("Enter the last blocked date")
                .withValidator(date -> startDate.getValue() == null || !date.isBefore(startDate.getValue()),
                        "The last blocked date must be on or after the first blocked date")
                .bind(RoomBlockInput::getEndDate, RoomBlockInput::setEndDate);
        // UC-018 A1: The reason has at most 200 characters.
        binder.forField(reason)
                .withValidator(value -> value == null || value.strip().length() <= RoomBlockService.MAX_REASON_LENGTH,
                        "The reason can have at most " + RoomBlockService.MAX_REASON_LENGTH + " characters")
                .bind(RoomBlockInput::getReason, RoomBlockInput::setReason);
        startDate.addValueChangeListener(_ -> {
            if (endDate.getValue() != null) {
                binder.validate();
            }
        });

        FormLayout form = new FormLayout(room, startDate, endDate, reason);
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("30rem", 3));
        form.setColspan(reason, 3);

        // Step 4
        Button saveButton = new Button("Save", _ -> save());
        saveButton.addThemeVariants(ButtonVariant.PRIMARY);
        // A5: the manager leaves without saving
        Button discardButton = new Button("Discard", _ -> closeEditor());
        discardButton.addThemeVariants(ButtonVariant.TERTIARY);

        editor.setPadding(false);
        editor.add(new H2("New room block"), form, new HorizontalLayout(saveButton, discardButton));
        editor.setVisible(false);
    }

    /**
     * Step 3.
     */
    private void edit() {
        message.removeAll();
        room.setItems(service.rooms());
        LocalDate today = service.today();
        startDate.setMin(today);
        endDate.setMin(today);
        input = new RoomBlockInput();
        input.setReason("");
        binder.setBean(input);
        editor.setVisible(true);
        room.focus();
    }

    /**
     * Steps 4 to 7, A1 to A4.
     */
    private void save() {
        message.removeAll();
        // A1: invalid values are marked; the entered values are kept
        if (!binder.validate().isOk()) {
            return;
        }
        BlockableRoom selectedRoom = input.getRoom();
        RoomBlockData data = new RoomBlockData(selectedRoom.id(), input.getStartDate(), input.getEndDate(),
                input.getReason());
        try {
            service.block(data);
        } catch (RoomBlockOverlapException e) {
            // A2: the overlapping block
            RoomBlockOverview block = e.overlappingBlock();
            showMessage(OVERLAP.formatted(block.roomNumber(), dateFormatter.format(block.startDate()),
                    dateFormatter.format(block.endDate())), true);
            return;
        } catch (RoomOccupiedDuringBlockException e) {
            // A3: the reservation
            showMessage(OCCUPIED.formatted(selectedRoom.roomNumber(), e.reservationCode()), true);
            return;
        } catch (ReservationsWouldLoseRoomException e) {
            // A4: the affected nights
            String nights = e.nights().stream().map(dateFormatter::format).collect(Collectors.joining(", "));
            showMessage(WOULD_LOSE_ROOM.formatted(e.roomTypeName(), nights), true);
            return;
        }
        // Step 7: the updated list
        closeEditor();
        showMessage(SAVED.formatted(selectedRoom.roomNumber(), dateFormatter.format(data.startDate()),
                dateFormatter.format(data.endDate())), false);
    }

    private void closeEditor() {
        editor.setVisible(false);
        binder.setBean(null);
        refreshGrid();
    }

    private void refreshGrid() {
        grid.setItems(service.roomBlocks());
    }

    private void showMessage(String text, boolean error) {
        Paragraph paragraph = new Paragraph(text);
        paragraph.setId("room-block-message");
        if (error) {
            paragraph.getElement().setAttribute("role", "alert");
            paragraph.addClassNames(LumoUtility.TextColor.ERROR);
        }
        message.removeAll();
        message.add(paragraph);
    }

    private static class RoomBlockInput {
        private BlockableRoom room;
        private LocalDate startDate;
        private LocalDate endDate;
        private String reason;

        BlockableRoom getRoom() {
            return room;
        }

        void setRoom(BlockableRoom room) {
            this.room = room;
        }

        LocalDate getStartDate() {
            return startDate;
        }

        void setStartDate(LocalDate startDate) {
            this.startDate = startDate;
        }

        LocalDate getEndDate() {
            return endDate;
        }

        void setEndDate(LocalDate endDate) {
            this.endDate = endDate;
        }

        String getReason() {
            return reason;
        }

        void setReason(String reason) {
            this.reason = reason;
        }
    }
}
