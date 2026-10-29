package ch.martinelli.edu.hrs.roomcatalog;

import ch.martinelli.edu.hrs.db.tables.records.RoomTypeRecord;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("Room types")
@Route("room-types")
public class RoomTypeView extends HorizontalLayout {

    private final RoomCatalogService roomCatalogService;

    private final Grid<RoomTypeRecord> grid = new Grid<>();
    private final TextField name = new TextField("Name");
    private final Binder<RoomTypeRecord> binder = new Binder<>();
    private final Button delete = new Button("Delete", _ -> confirmDelete());

    // null while a new room type is being entered
    private RoomTypeRecord selected;

    public RoomTypeView(RoomCatalogService roomCatalogService) {
        this.roomCatalogService = roomCatalogService;
        setSizeFull();

        grid.addColumn(RoomTypeRecord::getName).setHeader("Name");
        grid.addColumn(RoomTypeRecord::getCapacity).setHeader("Capacity");
        grid.addColumn(RoomTypeRecord::getNightlyRate).setHeader("Nightly rate (CHF)");
        grid.asSingleSelect().addValueChangeListener(e -> edit(e.getValue()));

        var capacity = new IntegerField("Capacity");
        var nightlyRate = new BigDecimalField("Nightly rate (CHF)");
        bindFields(capacity, nightlyRate);

        var save = new Button("Save", _ -> save());
        save.addThemeVariants(ButtonVariant.PRIMARY);
        delete.addThemeVariants(ButtonVariant.ERROR);
        var form = new VerticalLayout(new FormLayout(name, capacity, nightlyRate),
                new HorizontalLayout(save, new Button("New", _ -> grid.deselectAll()), delete));
        form.setWidth("24rem");

        add(grid, form);
        refresh();
    }

    private void bindFields(IntegerField capacity, BigDecimalField nightlyRate) {
        name.setRequiredIndicatorVisible(true);
        binder.forField(name)
                .withConverter(String::trim, v -> v == null ? "" : v)
                .withValidator(v -> !v.isEmpty(), "Name is required")
                .withValidator(v -> v.length() <= 50, "Name can have at most 50 characters")
                .bind(RoomTypeRecord::getName, RoomTypeRecord::setName);
        binder.forField(capacity)
                .asRequired("Capacity is required")
                .withValidator(v -> v >= 1 && v <= 10, "Capacity must be between 1 and 10")
                .bind(RoomTypeRecord::getCapacity, RoomTypeRecord::setCapacity);
        binder.forField(nightlyRate)
                .asRequired("Nightly rate is required")
                .withValidator(v -> v.signum() > 0, "Nightly rate must be greater than 0")
                .withValidator(v -> v.stripTrailingZeros().scale() <= 2, "Nightly rate can have at most two decimals")
                .bind(RoomTypeRecord::getNightlyRate, RoomTypeRecord::setNightlyRate);
    }

    private void edit(RoomTypeRecord roomType) {
        selected = roomType;
        binder.readBean(roomType);
        delete.setEnabled(roomType != null);
    }

    private void save() {
        // write into a fresh record so a failed save leaves the grid item untouched
        var roomType = new RoomTypeRecord();
        if (!binder.writeBeanIfValid(roomType)) {
            return;
        }
        if (selected != null) {
            roomType.setId(selected.getId());
        }
        try {
            roomCatalogService.saveRoomType(roomType);
            refresh();
        } catch (RoomTypeNameExistsException e) {
            name.setErrorMessage(e.getMessage());
            name.setInvalid(true);
        } catch (RoomTypeNotFoundException e) {
            Notification.show(e.getMessage());
            refresh();
        }
    }

    private void confirmDelete() {
        var dialog = new ConfirmDialog("Delete room type", "Delete room type \"%s\"?".formatted(selected.getName()),
                "Delete", _ -> delete(selected.getId()));
        dialog.setCancelable(true);
        dialog.setConfirmButtonTheme("error primary");
        dialog.open();
    }

    private void delete(long id) {
        try {
            roomCatalogService.deleteRoomType(id);
            refresh();
        } catch (RoomTypeInUseException e) {
            Notification.show(e.getMessage());
        } catch (RoomTypeNotFoundException e) {
            Notification.show(e.getMessage());
            refresh();
        }
    }

    private void refresh() {
        grid.setItems(roomCatalogService.findRoomTypes());
        edit(null);
    }
}
