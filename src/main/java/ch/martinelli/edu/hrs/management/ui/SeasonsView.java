package ch.martinelli.edu.hrs.management.ui;

import ch.martinelli.edu.hrs.management.SeasonData;
import ch.martinelli.edu.hrs.management.SeasonNameUsedException;
import ch.martinelli.edu.hrs.management.SeasonOverlapException;
import ch.martinelli.edu.hrs.management.SeasonOverview;
import ch.martinelli.edu.hrs.management.SeasonRoomType;
import ch.martinelli.edu.hrs.management.SeasonService;
import ch.martinelli.edu.hrs.staff.StaffRole;
import ch.martinelli.edu.hrs.staff.ui.StaffLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * UC-025 Maintain Seasonal Rates: the manager creates, changes and deletes seasons with a season rate per room type.
 * <p>
 * Nothing is stored before the manager saves, so leaving the season discards the entered values (A5).
 */
@Route(value = "seasons", layout = StaffLayout.class)
@PageTitle("Seasons")
// NFR-009: Only the manager can maintain seasonal rates.
@RolesAllowed(StaffRole.MANAGER_ROLE)
public class SeasonsView extends VerticalLayout {

    static final String NAME_USED = "The name is already used by another season.";
    static final String OVERLAP = "The season overlaps the season %s from %s to %s.";
    static final String SAVED = "Season %s is saved.";
    static final String DELETED = "Season %s is deleted.";
    static final String RATE_INVALID = "The season rate must be between 0 and 99,999,999.99, with at most 2 decimals";

    private final SeasonService service;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
    private final Grid<SeasonOverview> grid = new Grid<>();
    private final Div message = new Div();

    // Step 4
    private final VerticalLayout editor = new VerticalLayout();
    private final H2 editorTitle = new H2();
    private final TextField name = new TextField("Name");
    private final DatePicker firstNight = new DatePicker("First night");
    private final DatePicker lastNight = new DatePicker("Last night");
    private final FormLayout rateForm = new FormLayout();
    private final Button deleteButton = new Button("Delete");
    private final Map<Long, BigDecimalField> rateFields = new HashMap<>();
    private Binder<SeasonInput> binder;

    private SeasonOverview editedSeason;
    private SeasonInput input;

    public SeasonsView(SeasonService service) {
        this.service = service;
        add(new H1("Seasons"));

        // Step 3: create a new season
        Button newButton = new Button("New season", _ -> edit(null));
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
     * Step 2: name, first night, last night and number of room types with a season rate, ordered by first night.
     */
    private void createGrid() {
        grid.setId("seasons");
        grid.addColumn(SeasonOverview::name).setHeader("Name").setAutoWidth(true);
        grid.addColumn(season -> dateFormatter.format(season.firstNight())).setHeader("First night")
                .setAutoWidth(true);
        grid.addColumn(season -> dateFormatter.format(season.lastNight())).setHeader("Last night")
                .setAutoWidth(true);
        grid.addColumn(SeasonOverview::roomTypesWithRate).setHeader("Room types with season rate")
                .setAutoWidth(true);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setAllRowsVisible(true);
        // Step 3: select an existing season
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                edit(event.getValue());
            }
        });
    }

    private void createEditor() {
        name.setMaxLength(SeasonService.MAX_NAME_LENGTH);
        name.setRequiredIndicatorVisible(true);
        firstNight.setRequiredIndicatorVisible(true);
        lastNight.setRequiredIndicatorVisible(true);
        lastNight.setHelperText("On or after the first night");
        firstNight.addValueChangeListener(_ -> {
            if (binder != null && lastNight.getValue() != null) {
                binder.validate();
            }
        });

        FormLayout form = new FormLayout(name, firstNight, lastNight);
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("30rem", 3));
        rateForm.setId("season-rates");
        rateForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("30rem", 3));

        // Step 6
        Button saveButton = new Button("Save", _ -> save());
        saveButton.addThemeVariants(ButtonVariant.PRIMARY);
        // A4: delete the selected existing season
        deleteButton.addThemeVariants(ButtonVariant.ERROR);
        deleteButton.addClickListener(_ -> confirmDeletion());
        // A5: the manager leaves the season without saving
        Button discardButton = new Button("Discard", _ -> closeEditor());
        discardButton.addThemeVariants(ButtonVariant.TERTIARY);

        Paragraph rateHint = new Paragraph("A room type left without a season rate keeps its normal rate "
                + "during the season.");
        rateHint.addClassNames(LumoUtility.TextColor.SECONDARY);

        editor.setPadding(false);
        editor.add(editorTitle, form, new H3("Season rates (CHF)"), rateHint, rateForm,
                new HorizontalLayout(saveButton, deleteButton, discardButton));
        editor.setVisible(false);
    }

    /**
     * Steps 3 and 4.
     */
    private void edit(SeasonOverview season) {
        message.removeAll();
        editedSeason = season;
        Long seasonId = season == null ? null : season.id();
        List<SeasonRoomType> roomTypes = service.roomTypes(seasonId);
        input = SeasonInput.of(season, roomTypes);
        editorTitle.setText(season == null ? "New season" : "Season " + season.name());
        deleteButton.setVisible(season != null);
        createBinder(roomTypes);
        binder.setBean(input);
        editor.setVisible(true);
        name.focus();
    }

    /**
     * Step 4: one field per room type with its normal rate; a new binder each time, because the room types differ.
     */
    private void createBinder(List<SeasonRoomType> roomTypes) {
        binder = new Binder<>();
        // UC-025 A1: The name is required and at most 50 characters.
        binder.forField(name)
                .asRequired("Enter the name")
                .withValidator(value -> !value.isBlank(), "Enter the name")
                .withValidator(value -> value.strip().length() <= SeasonService.MAX_NAME_LENGTH,
                        "The name can have at most " + SeasonService.MAX_NAME_LENGTH + " characters")
                .bind(SeasonInput::getName, SeasonInput::setName);
        // UC-025 A1: The first night is required.
        binder.forField(firstNight)
                .asRequired("Enter the first night")
                .bind(SeasonInput::getFirstNight, SeasonInput::setFirstNight);
        // UC-025 A1: The last night is required and not before the first night.
        binder.forField(lastNight)
                .asRequired("Enter the last night")
                .withValidator(date -> firstNight.getValue() == null || !date.isBefore(firstNight.getValue()),
                        "The last night must be on or after the first night")
                .bind(SeasonInput::getLastNight, SeasonInput::setLastNight);

        rateForm.removeAll();
        rateFields.clear();
        for (SeasonRoomType roomType : roomTypes) {
            BigDecimalField field = new BigDecimalField(roomType.name());
            field.setId("season-rate-" + roomType.roomTypeId());
            field.setClearButtonVisible(true);
            field.setHelperText("Normal rate " + chf(roomType.normalRate())
                    + (roomType.active() ? "" : " · inactive room type"));
            // UC-025 BR-006: A season rate is between 0 and 99,999,999.99 CHF.
            binder.forField(field)
                    .withValidator(rate -> rate == null || SeasonService.isValidSeasonRate(rate), RATE_INVALID)
                    .bind(i -> i.getSeasonRates().get(roomType.roomTypeId()),
                            (i, rate) -> i.getSeasonRates().put(roomType.roomTypeId(), rate));
            rateFields.put(roomType.roomTypeId(), field);
            rateForm.add(field);
        }
    }

    /**
     * Steps 6 to 9, A1 to A3.
     */
    private void save() {
        message.removeAll();
        // A1: invalid values are marked; the entered values are kept
        if (!binder.validate().isOk()) {
            return;
        }
        SeasonData data = new SeasonData(input.getName(), input.getFirstNight(), input.getLastNight(),
                new HashMap<>(input.getSeasonRates()));
        try {
            service.save(editedSeason == null ? null : editedSeason.id(), data);
        } catch (SeasonNameUsedException _) {
            // A2: continue at step 5
            name.setErrorMessage(NAME_USED);
            name.setInvalid(true);
            showMessage(NAME_USED, true);
            return;
        } catch (SeasonOverlapException e) {
            // A3: name the other season and its first and last night
            SeasonOverview other = e.otherSeason();
            showMessage(OVERLAP.formatted(other.name(), dateFormatter.format(other.firstNight()),
                    dateFormatter.format(other.lastNight())), true);
            return;
        }
        // Step 9: the updated list
        String savedName = data.name().strip();
        closeEditor();
        showMessage(SAVED.formatted(savedName), false);
    }

    /**
     * A4 steps 1 and 2.
     */
    private void confirmDeletion() {
        if (editedSeason == null) {
            return;
        }
        SeasonOverview season = editedSeason;
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete season " + season.name() + "?");
        dialog.setText("The season and its season rates are removed. Existing reservations keep their prices.");
        dialog.setConfirmText("Delete season");
        dialog.setConfirmButtonTheme("error primary");
        dialog.setCancelable(true);
        dialog.setCancelText("Keep season");
        dialog.addConfirmListener(_ -> {
            // A4 steps 3 and 4
            service.delete(season.id());
            closeEditor();
            showMessage(DELETED.formatted(season.name()), false);
        });
        dialog.open();
    }

    private void closeEditor() {
        editor.setVisible(false);
        editedSeason = null;
        if (binder != null) {
            binder.setBean(null);
        }
        grid.asSingleSelect().clear();
        refreshGrid();
    }

    private void refreshGrid() {
        grid.setItems(service.seasons());
    }

    private void showMessage(String text, boolean error) {
        Paragraph paragraph = new Paragraph(text);
        paragraph.setId("season-message");
        if (error) {
            paragraph.getElement().setAttribute("role", "alert");
            paragraph.addClassNames(LumoUtility.TextColor.ERROR);
        }
        message.removeAll();
        message.add(paragraph);
    }

    private static String chf(BigDecimal amount) {
        return "CHF " + amount.setScale(2).toPlainString();
    }

    private static class SeasonInput {
        private String name;
        private LocalDate firstNight;
        private LocalDate lastNight;
        private final Map<Long, BigDecimal> seasonRates = new HashMap<>();

        static SeasonInput of(SeasonOverview season, List<SeasonRoomType> roomTypes) {
            SeasonInput input = new SeasonInput();
            input.name = season == null ? "" : season.name();
            input.firstNight = season == null ? null : season.firstNight();
            input.lastNight = season == null ? null : season.lastNight();
            for (SeasonRoomType roomType : roomTypes) {
                if (roomType.seasonRate() != null) {
                    input.seasonRates.put(roomType.roomTypeId(), roomType.seasonRate());
                }
            }
            return input;
        }

        String getName() {
            return name;
        }

        void setName(String name) {
            this.name = name;
        }

        LocalDate getFirstNight() {
            return firstNight;
        }

        void setFirstNight(LocalDate firstNight) {
            this.firstNight = firstNight;
        }

        LocalDate getLastNight() {
            return lastNight;
        }

        void setLastNight(LocalDate lastNight) {
            this.lastNight = lastNight;
        }

        Map<Long, BigDecimal> getSeasonRates() {
            return seasonRates;
        }
    }
}
