package ch.martinelli.edu.hrs.management.ui;

import ch.martinelli.edu.hrs.management.AffectedReservation;
import ch.martinelli.edu.hrs.management.MaxGuestsBelowReservationsException;
import ch.martinelli.edu.hrs.management.RoomTypeData;
import ch.martinelli.edu.hrs.management.RoomTypeNameUsedException;
import ch.martinelli.edu.hrs.management.RoomTypeOverview;
import ch.martinelli.edu.hrs.management.RoomTypeService;
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
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;

import java.math.BigDecimal;
import java.util.stream.Collectors;

/**
 * UC-016 Maintain Room Types: the manager creates and edits room types.
 * <p>
 * Nothing is stored before the manager saves, so leaving the room type discards the entered values (A4).
 */
@Route(value = "room-types", layout = StaffLayout.class)
@PageTitle("Room Types")
// NFR-009: Only the manager can maintain room types.
@RolesAllowed(StaffRole.MANAGER_ROLE)
public class RoomTypesView extends VerticalLayout {

    static final String NAME_USED = "The name is already used by another room type.";
    static final String MAX_BELOW_RESERVATIONS = "The maximum number of guests cannot be lower than the number of "
            + "guests of these reservations: %s.";
    static final String SAVED = "Room type %s is saved.";

    private final RoomTypeService service;
    private final Grid<RoomTypeOverview> grid = new Grid<>();
    private final Div message = new Div();

    // Step 4
    private final VerticalLayout editor = new VerticalLayout();
    private final H2 editorTitle = new H2();
    private final TextField name = new TextField("Name");
    private final TextArea description = new TextArea("Description");
    private final IntegerField maxGuests = new IntegerField("Maximum number of guests");
    private final BigDecimalField nightlyRate = new BigDecimalField("Normal rate (CHF)");
    private final Checkbox active = new Checkbox("Active");
    private final Binder<RoomTypeInput> binder = new Binder<>();

    private Long editedRoomTypeId;
    private RoomTypeInput input;

    public RoomTypesView(RoomTypeService service) {
        this.service = service;
        add(new H1("Room Types"));

        // Step 3: create a new room type
        Button newButton = new Button("New room type", _ -> edit(null));
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
     * Step 2: name, maximum number of guests, normal rate, number of active rooms and whether active.
     */
    private void createGrid() {
        grid.setId("room-types");
        grid.addColumn(RoomTypeOverview::name).setHeader("Name").setAutoWidth(true);
        grid.addColumn(RoomTypeOverview::maxGuests).setHeader("Max. guests").setAutoWidth(true);
        grid.addColumn(roomType -> chf(roomType.nightlyRate())).setHeader("Normal rate").setAutoWidth(true);
        grid.addColumn(RoomTypeOverview::activeRooms).setHeader("Active rooms").setAutoWidth(true);
        grid.addColumn(roomType -> roomType.active() ? "Active" : "Inactive").setHeader("Status").setAutoWidth(true);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setAllRowsVisible(true);
        // Step 3: select an existing room type
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                edit(event.getValue());
            }
        });
    }

    private void createEditor() {
        name.setMaxLength(RoomTypeService.MAX_NAME_LENGTH);
        name.setRequiredIndicatorVisible(true);
        description.setMaxLength(RoomTypeService.MAX_DESCRIPTION_LENGTH);
        maxGuests.setStepButtonsVisible(true);
        maxGuests.setRequiredIndicatorVisible(true);
        maxGuests.setHelperText("Between " + RoomTypeService.MIN_GUESTS + " and " + RoomTypeService.MAX_GUESTS);
        nightlyRate.setRequiredIndicatorVisible(true);

        // UC-016 A1: The name is required and at most 50 characters.
        binder.forField(name)
                .asRequired("Enter the name")
                .withValidator(value -> !value.isBlank(), "Enter the name")
                .withValidator(value -> value.strip().length() <= RoomTypeService.MAX_NAME_LENGTH,
                        "The name can have at most " + RoomTypeService.MAX_NAME_LENGTH + " characters")
                .bind(RoomTypeInput::getName, RoomTypeInput::setName);
        // UC-016 A1: The description is at most 500 characters.
        binder.forField(description)
                .withValidator(value -> value == null || value.strip().length() <= RoomTypeService.MAX_DESCRIPTION_LENGTH,
                        "The description can have at most " + RoomTypeService.MAX_DESCRIPTION_LENGTH + " characters")
                .bind(RoomTypeInput::getDescription, RoomTypeInput::setDescription);
        // UC-016 BR-003: The maximum number of guests is between 1 and 10.
        binder.forField(maxGuests)
                .asRequired("Enter the maximum number of guests")
                .withValidator(value -> value >= RoomTypeService.MIN_GUESTS && value <= RoomTypeService.MAX_GUESTS,
                        "The maximum number of guests must be between " + RoomTypeService.MIN_GUESTS + " and "
                                + RoomTypeService.MAX_GUESTS)
                .bind(RoomTypeInput::getMaxGuests, RoomTypeInput::setMaxGuests);
        // UC-016 A1: The normal rate is required and not negative.
        binder.forField(nightlyRate)
                .asRequired("Enter the normal rate")
                .withValidator(RoomTypeService::isValidNightlyRate,
                        "The normal rate must be 0 or more, with at most 2 decimals")
                .bind(RoomTypeInput::getNightlyRate, RoomTypeInput::setNightlyRate);
        binder.forField(active).bind(RoomTypeInput::isActive, RoomTypeInput::setActive);

        FormLayout form = new FormLayout(name, maxGuests, nightlyRate, active, description);
        form.setColspan(description, 2);
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("30rem", 2));

        // Step 5
        Button saveButton = new Button("Save", _ -> save());
        saveButton.addThemeVariants(ButtonVariant.PRIMARY);
        // A4: the manager leaves the room type without saving
        Button discardButton = new Button("Discard", _ -> closeEditor());
        discardButton.addThemeVariants(ButtonVariant.TERTIARY);

        editor.setPadding(false);
        editor.add(editorTitle, form, new HorizontalLayout(saveButton, discardButton));
        editor.setVisible(false);
    }

    /**
     * Steps 3 and 4.
     */
    private void edit(RoomTypeOverview roomType) {
        message.removeAll();
        editedRoomTypeId = roomType == null ? null : roomType.id();
        input = roomType == null ? RoomTypeInput.empty() : RoomTypeInput.of(roomType);
        editorTitle.setText(roomType == null ? "New room type" : "Room type " + roomType.name());
        binder.setBean(input);
        editor.setVisible(true);
        name.focus();
    }

    /**
     * Steps 5 to 8, A1 to A3.
     */
    private void save() {
        message.removeAll();
        // A1: invalid values are marked; the entered values are kept
        if (!binder.validate().isOk()) {
            return;
        }
        RoomTypeData data = new RoomTypeData(input.getName(), input.getDescription(), input.getMaxGuests(),
                input.getNightlyRate(), input.isActive());
        try {
            service.save(editedRoomTypeId, data);
        } catch (RoomTypeNameUsedException _) {
            // A2: continue at step 4
            name.setErrorMessage(NAME_USED);
            name.setInvalid(true);
            showMessage(NAME_USED, true);
            return;
        } catch (MaxGuestsBelowReservationsException e) {
            // A3: list the affected reservations and refuse the change
            String reservations = e.reservations().stream()
                    .map(RoomTypesView::reservationText)
                    .collect(Collectors.joining(", "));
            maxGuests.setInvalid(true);
            maxGuests.setErrorMessage("Too low for existing reservations");
            showMessage(MAX_BELOW_RESERVATIONS.formatted(reservations), true);
            return;
        }
        // Step 8: the updated list
        String savedName = data.name().strip();
        closeEditor();
        showMessage(SAVED.formatted(savedName), false);
    }

    private void closeEditor() {
        editor.setVisible(false);
        editedRoomTypeId = null;
        binder.setBean(null);
        refreshGrid();
    }

    private void refreshGrid() {
        grid.setItems(service.roomTypes());
    }

    private void showMessage(String text, boolean error) {
        Paragraph paragraph = new Paragraph(text);
        paragraph.setId("room-type-message");
        if (error) {
            paragraph.getElement().setAttribute("role", "alert");
            paragraph.addClassNames(LumoUtility.TextColor.ERROR);
        }
        message.removeAll();
        message.add(paragraph);
    }

    private static String reservationText(AffectedReservation reservation) {
        return reservation.reservationCode() + " (" + reservation.numberOfGuests()
                + (reservation.numberOfGuests() == 1 ? " guest)" : " guests)");
    }

    private static String chf(BigDecimal amount) {
        return "CHF " + amount.setScale(2).toPlainString();
    }

    private static class RoomTypeInput {
        private String name;
        private String description;
        private Integer maxGuests;
        private BigDecimal nightlyRate;
        private boolean active;

        static RoomTypeInput empty() {
            RoomTypeInput input = new RoomTypeInput();
            input.name = "";
            input.description = "";
            input.active = true;
            return input;
        }

        static RoomTypeInput of(RoomTypeOverview roomType) {
            RoomTypeInput input = new RoomTypeInput();
            input.name = roomType.name();
            input.description = roomType.description() == null ? "" : roomType.description();
            input.maxGuests = roomType.maxGuests();
            input.nightlyRate = roomType.nightlyRate();
            input.active = roomType.active();
            return input;
        }

        String getName() {
            return name;
        }

        void setName(String name) {
            this.name = name;
        }

        String getDescription() {
            return description;
        }

        void setDescription(String description) {
            this.description = description;
        }

        Integer getMaxGuests() {
            return maxGuests;
        }

        void setMaxGuests(Integer maxGuests) {
            this.maxGuests = maxGuests;
        }

        BigDecimal getNightlyRate() {
            return nightlyRate;
        }

        void setNightlyRate(BigDecimal nightlyRate) {
            this.nightlyRate = nightlyRate;
        }

        boolean isActive() {
            return active;
        }

        void setActive(boolean active) {
            this.active = active;
        }
    }
}
