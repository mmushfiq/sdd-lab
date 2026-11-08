package ch.martinelli.edu.hrs.room;

import ch.martinelli.edu.hrs.core.BusinessRuleException;
import ch.martinelli.edu.hrs.core.ui.MainLayout;
import ch.martinelli.edu.hrs.db.tables.records.RoomTypeRecord;
import ch.martinelli.edu.hrs.db.tables.records.SeasonalRateRecord;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ch.martinelli.edu.hrs.core.ui.Notifications.showError;

/**
 * 003 US1: the Manager maintains seasonal rates (contracts/ui-changes.md).
 */
@Route(value = "manager/seasonal-rates", layout = MainLayout.class)
@PageTitle("Seasonal rates | Hotel Reservation System")
@Menu(title = "Seasonal rates", order = 52.5)
@RolesAllowed("MANAGER")
public class SeasonalRateView extends VerticalLayout {

    private final transient SeasonalRateService seasonalRateService;
    private final Map<Long, RoomTypeRecord> roomTypes;
    private final Grid<SeasonalRateRecord> grid = new Grid<>();
    private final Binder<SeasonalRateRecord> binder = new Binder<>();
    private final Button delete = new Button("Delete", _ -> delete());
    private SeasonalRateRecord current = new SeasonalRateRecord();

    public SeasonalRateView(SeasonalRateService seasonalRateService, RoomTypeService roomTypeService) {
        this.seasonalRateService = seasonalRateService;
        var allTypes = roomTypeService.findAll();
        roomTypes = allTypes.stream().collect(Collectors.toMap(RoomTypeRecord::getId, Function.identity()));

        grid.setId("seasonal-rates");
        grid.addColumn(r -> roomTypes.get(r.getRoomTypeId()).getName()).setHeader("Room type");
        grid.addColumn(SeasonalRateRecord::getName).setHeader("Name");
        grid.addColumn(SeasonalRateRecord::getStartDate).setHeader("Start date");
        grid.addColumn(SeasonalRateRecord::getEndDate).setHeader("End date");
        grid.addColumn(SeasonalRateRecord::getNightlyRate).setHeader("Nightly rate");
        grid.asSingleSelect().addValueChangeListener(e -> edit(e.getValue()));

        var roomType = new ComboBox<RoomTypeRecord>("Room type");
        roomType.setItems(allTypes);
        roomType.setItemLabelGenerator(RoomTypeRecord::getName);
        var name = new TextField("Name");
        name.setMaxLength(50);
        var startDate = new DatePicker("Start date");
        var endDate = new DatePicker("End date");
        var nightlyRate = new BigDecimalField("Nightly rate");
        binder.forField(roomType).asRequired("Room type is required")
                .bind(r -> roomTypes.get(r.getRoomTypeId()), (r, t) -> r.setRoomTypeId(t.getId()));
        binder.forField(name).asRequired("Name is required")
                .bind(SeasonalRateRecord::getName, SeasonalRateRecord::setName);
        binder.forField(startDate).asRequired("Start date is required")
                .bind(SeasonalRateRecord::getStartDate, SeasonalRateRecord::setStartDate);
        binder.forField(endDate).asRequired("End date is required")
                .bind(SeasonalRateRecord::getEndDate, SeasonalRateRecord::setEndDate);
        binder.forField(nightlyRate).asRequired("Nightly rate is required")
                .bind(SeasonalRateRecord::getNightlyRate, SeasonalRateRecord::setNightlyRate);

        var save = new Button("Save", _ -> save());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        var newRate = new Button("New seasonal rate", _ -> clearForm());

        add(grid, new FormLayout(roomType, name, startDate, endDate, nightlyRate),
                new HorizontalLayout(save, delete, newRate));
        refresh();
        edit(null);
    }

    private void edit(SeasonalRateRecord rate) {
        current = rate != null ? rate : new SeasonalRateRecord();
        binder.readBean(current);
        delete.setEnabled(rate != null);
    }

    private void save() {
        if (binder.writeBeanIfValid(current)) {
            try {
                seasonalRateService.save(current);
                refresh();
                clearForm();
            } catch (BusinessRuleException e) {
                showError(e.getMessage());
            }
        }
    }

    private void delete() {
        seasonalRateService.delete(current.getId());
        refresh();
        clearForm();
    }

    // Clearing an empty selection fires no event, so the form is reset here too
    private void clearForm() {
        grid.asSingleSelect().clear();
        edit(null);
    }

    private void refresh() {
        grid.setItems(seasonalRateService.findAll());
    }
}
