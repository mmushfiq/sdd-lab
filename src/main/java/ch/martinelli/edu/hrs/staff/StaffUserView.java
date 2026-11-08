package ch.martinelli.edu.hrs.staff;

import ch.martinelli.edu.hrs.core.BusinessRuleException;
import ch.martinelli.edu.hrs.core.security.Role;
import ch.martinelli.edu.hrs.core.ui.MainLayout;
import ch.martinelli.edu.hrs.db.tables.records.StaffUserRecord;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import static ch.martinelli.edu.hrs.core.ui.Notifications.showError;

/**
 * The Manager creates staff users, changes their role, resets their password and deactivates them (FR-025).
 */
@Route(value = "manager/staff", layout = MainLayout.class)
@PageTitle("Staff | Hotel Reservation System")
@Menu(title = "Staff", order = 53)
@RolesAllowed("MANAGER")
public class StaffUserView extends VerticalLayout {

    private final transient StaffUserService staffUserService;
    private final Grid<StaffUserRecord> grid = new Grid<>();
    private final TextField username = new TextField("Username");
    private final Select<Role> role = new Select<>();
    private final PasswordField password = new PasswordField("Password");
    private final Button create = new Button("Create", _ -> create());
    private final Button saveRole = new Button("Save role", _ -> saveRole());
    private final Button resetPassword = new Button("Reset password", _ -> resetPassword());
    private final Button deactivate = new Button("Deactivate", _ -> deactivate());

    public StaffUserView(StaffUserService staffUserService) {
        this.staffUserService = staffUserService;

        grid.addColumn(StaffUserRecord::getUsername).setHeader("Username");
        grid.addColumn(u -> label(Role.valueOf(u.getRole()))).setHeader("Role");
        grid.addColumn(u -> Boolean.TRUE.equals(u.getActive()) ? "Yes" : "No").setHeader("Active");
        grid.asSingleSelect().addValueChangeListener(e -> select(e.getValue()));

        username.setMaxLength(50);
        role.setLabel("Role");
        role.setItems(Role.values());
        role.setItemLabelGenerator(StaffUserView::label);
        create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        deactivate.addThemeVariants(ButtonVariant.LUMO_ERROR);
        var newUser = new Button("New", _ -> grid.asSingleSelect().clear());

        add(grid, new FormLayout(username, role, password),
                new HorizontalLayout(create, saveRole, resetPassword, deactivate, newUser));
        refresh();
        select(null);
    }

    private void select(StaffUserRecord user) {
        var isNew = user == null;
        username.setValue(isNew ? "" : user.getUsername());
        username.setReadOnly(!isNew);
        role.setValue(isNew ? null : Role.valueOf(user.getRole()));
        password.clear();
        create.setVisible(isNew);
        saveRole.setVisible(!isNew);
        resetPassword.setVisible(!isNew);
        deactivate.setVisible(!isNew && Boolean.TRUE.equals(user.getActive()));
    }

    private void create() {
        try {
            staffUserService.create(username.getValue().trim(), role.getValue(), password.getValue());
            refresh();
            select(null);
        } catch (BusinessRuleException e) {
            showError(e.getMessage());
        }
    }

    private void saveRole() {
        try {
            staffUserService.changeRole(selectedId(), role.getValue());
            refresh();
        } catch (BusinessRuleException e) {
            showError(e.getMessage());
        }
    }

    private void resetPassword() {
        try {
            staffUserService.resetPassword(selectedId(), password.getValue());
            password.clear();
        } catch (BusinessRuleException e) {
            showError(e.getMessage());
        }
    }

    private void deactivate() {
        staffUserService.deactivate(selectedId());
        refresh();
        select(null);
    }

    private long selectedId() {
        return grid.asSingleSelect().getValue().getId();
    }

    private void refresh() {
        grid.setItems(staffUserService.findAll());
    }

    private static String label(Role role) {
        var name = role.name();
        return name.charAt(0) + name.substring(1).toLowerCase();
    }
}
