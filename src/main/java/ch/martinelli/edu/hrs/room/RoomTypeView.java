package ch.martinelli.edu.hrs.room;

import ch.martinelli.edu.hrs.core.BusinessRuleException;
import ch.martinelli.edu.hrs.core.ui.MainLayout;
import ch.martinelli.edu.hrs.db.tables.records.RoomTypeRecord;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.math.BigDecimal;

import static ch.martinelli.edu.hrs.core.ui.Notifications.showError;

@Route(value = "manager/room-types", layout = MainLayout.class)
@PageTitle("Room types | Hotel Reservation System")
@Menu(title = "Room types", order = 52)
@RolesAllowed("MANAGER")
public class RoomTypeView extends VerticalLayout {

    private final transient RoomTypeService roomTypeService;
    private final Grid<RoomTypeRecord> grid = new Grid<>();
    private final Binder<RoomTypeRecord> binder = new Binder<>();
    private RoomTypeRecord current = new RoomTypeRecord();

    public RoomTypeView(RoomTypeService roomTypeService) {
        this.roomTypeService = roomTypeService;

        grid.addColumn(RoomTypeRecord::getName).setHeader("Name");
        grid.addColumn(RoomTypeRecord::getCapacity).setHeader("Capacity");
        grid.addColumn(RoomTypeRecord::getNightlyRate).setHeader("Standard nightly rate");
        grid.asSingleSelect().addValueChangeListener(e -> edit(e.getValue()));

        var name = new TextField("Name");
        name.setMaxLength(50);
        var capacity = new IntegerField("Capacity");
        var nightlyRate = new BigDecimalField("Standard nightly rate");
        binder.forField(name).asRequired("Name is required")
                .bind(RoomTypeRecord::getName, RoomTypeRecord::setName);
        binder.forField(capacity).asRequired("Capacity is required")
                .withValidator(c -> c >= 1, "Capacity must be at least 1")
                .bind(RoomTypeRecord::getCapacity, RoomTypeRecord::setCapacity);
        binder.forField(nightlyRate).asRequired("Standard nightly rate is required")
                .withValidator(r -> r.compareTo(BigDecimal.ZERO) > 0, "Standard nightly rate must be greater than 0")
                .bind(RoomTypeRecord::getNightlyRate, RoomTypeRecord::setNightlyRate);

        var save = new Button("Save", _ -> save());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        var delete = new Button("Delete", _ -> delete());
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        var newType = new Button("New", _ -> clearForm());

        add(grid, new FormLayout(name, capacity, nightlyRate), new HorizontalLayout(save, delete, newType));
        refresh();
        edit(null);
    }

    private void edit(RoomTypeRecord roomType) {
        current = roomType != null ? roomType : new RoomTypeRecord();
        binder.readBean(current);
    }

    private void save() {
        if (binder.writeBeanIfValid(current)) {
            try {
                roomTypeService.save(current);
                refresh();
                clearForm();
            } catch (BusinessRuleException e) {
                showError(e.getMessage());
            }
        }
    }

    private void delete() {
        if (current.getId() == null) {
            return;
        }
        try {
            roomTypeService.delete(current.getId());
            refresh();
            clearForm();
        } catch (BusinessRuleException e) {
            showError(e.getMessage());
        }
    }

    // Clearing an empty selection fires no event, so the form is reset here too
    private void clearForm() {
        grid.asSingleSelect().clear();
        edit(null);
    }

    private void refresh() {
        grid.setItems(roomTypeService.findAll());
    }
}
