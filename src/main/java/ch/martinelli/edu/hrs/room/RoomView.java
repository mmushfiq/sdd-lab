package ch.martinelli.edu.hrs.room;

import ch.martinelli.edu.hrs.core.BusinessRuleException;
import ch.martinelli.edu.hrs.core.ui.MainLayout;
import ch.martinelli.edu.hrs.db.tables.records.RoomRecord;
import ch.martinelli.edu.hrs.db.tables.records.RoomTypeRecord;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
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

@Route(value = "manager/rooms", layout = MainLayout.class)
@PageTitle("Rooms | Hotel Reservation System")
@Menu(title = "Rooms", order = 51)
@RolesAllowed("MANAGER")
public class RoomView extends VerticalLayout {

    private final transient RoomService roomService;
    private final Map<Long, RoomTypeRecord> roomTypes;
    private final Grid<RoomRecord> grid = new Grid<>();
    private final Binder<RoomRecord> binder = new Binder<>();
    private RoomRecord current = new RoomRecord();

    public RoomView(RoomService roomService, RoomTypeService roomTypeService) {
        this.roomService = roomService;
        roomTypes = roomTypeService.findAll().stream()
                .collect(Collectors.toMap(RoomTypeRecord::getId, Function.identity()));

        grid.addColumn(RoomRecord::getRoomNumber).setHeader("Room number");
        grid.addColumn(r -> roomTypes.get(r.getRoomTypeId()).getName()).setHeader("Room type");
        grid.addColumn(r -> CleaningStatus.valueOf(r.getCleaningStatus()) == CleaningStatus.CLEAN ? "Clean" : "Dirty")
                .setHeader("Cleaning status");
        grid.asSingleSelect().addValueChangeListener(e -> edit(e.getValue()));

        var roomNumber = new TextField("Room number");
        roomNumber.setMaxLength(10);
        var roomType = new ComboBox<RoomTypeRecord>("Room type");
        roomType.setItems(roomTypeService.findAll());
        roomType.setItemLabelGenerator(RoomTypeRecord::getName);
        binder.forField(roomNumber).asRequired("Room number is required")
                .bind(RoomRecord::getRoomNumber, RoomRecord::setRoomNumber);
        binder.forField(roomType).asRequired("Room type is required")
                .bind(r -> roomTypes.get(r.getRoomTypeId()), (r, t) -> r.setRoomTypeId(t.getId()));

        var save = new Button("Save", _ -> save());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        var delete = new Button("Delete", _ -> delete());
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        var newRoom = new Button("New", _ -> grid.asSingleSelect().clear());

        add(grid, new FormLayout(roomNumber, roomType), new HorizontalLayout(save, delete, newRoom));
        refresh();
        edit(null);
    }

    private void edit(RoomRecord room) {
        current = room != null ? room : new RoomRecord();
        binder.readBean(current);
    }

    private void save() {
        if (binder.writeBeanIfValid(current)) {
            try {
                roomService.save(current);
                refresh();
                grid.asSingleSelect().clear();
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
            roomService.delete(current.getId());
            refresh();
            grid.asSingleSelect().clear();
        } catch (BusinessRuleException e) {
            showError(e.getMessage());
        }
    }

    private void refresh() {
        grid.setItems(roomService.findAll());
    }
}
