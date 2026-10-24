package ch.martinelli.edu.hrs.staff.ui;

import ch.martinelli.edu.hrs.staff.OwnAccountChangeException;
import ch.martinelli.edu.hrs.staff.StaffMemberData;
import ch.martinelli.edu.hrs.staff.StaffMemberOverview;
import ch.martinelli.edu.hrs.staff.StaffMemberService;
import ch.martinelli.edu.hrs.staff.StaffRole;
import ch.martinelli.edu.hrs.staff.UsernameUsedException;
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
import com.vaadin.flow.component.textfield.Autocomplete;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;

/**
 * UC-021 Maintain Staff Members: the manager creates, edits and deactivates staff members with their role.
 * <p>
 * Nothing is stored before the manager saves, so leaving the staff member discards the entered values (A6).
 * Passwords are never shown: the password field is always empty when the editor opens.
 */
@Route(value = "staff-members", layout = StaffLayout.class)
@PageTitle("Staff Members")
// NFR-009: Only the manager can maintain staff members.
@RolesAllowed(StaffRole.MANAGER_ROLE)
public class StaffMembersView extends VerticalLayout {

    static final String USERNAME_USED = "The username is already used by another staff member.";
    static final String OWN_ACCOUNT = "You cannot deactivate your own account or change your own role. "
            + "Another manager has to make this change.";
    static final String SAVED = "Staff member %s is saved.";
    static final String PASSWORD_TOO_SHORT = "The password must have at least "
            + StaffMemberService.MIN_PASSWORD_LENGTH + " characters";

    private final StaffMemberService service;
    private final String managerUsername;
    private final Grid<StaffMemberOverview> grid = new Grid<>();
    private final Div message = new Div();

    // Step 4
    private final VerticalLayout editor = new VerticalLayout();
    private final H2 editorTitle = new H2();
    private final TextField username = new TextField("Username");
    private final TextField firstName = new TextField("First name");
    private final TextField lastName = new TextField("Last name");
    private final Select<StaffRole> role = new Select<>();
    private final Checkbox active = new Checkbox("Active");
    private final PasswordField password = new PasswordField();
    private final Binder<StaffMemberInput> binder = new Binder<>();

    private Long editedStaffMemberId;
    private StaffMemberInput input;

    public StaffMembersView(StaffMemberService service, AuthenticationContext authenticationContext) {
        this.service = service;
        this.managerUsername = authenticationContext.getPrincipalName().orElse("");
        add(new H1("Staff Members"));

        // Step 3: create a new staff member
        Button newButton = new Button("New staff member", _ -> edit(null));
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
     * Step 2: username, first name, last name, role and whether active.
     */
    private void createGrid() {
        grid.setId("staff-members");
        grid.addColumn(StaffMemberOverview::username).setHeader("Username").setAutoWidth(true);
        grid.addColumn(StaffMemberOverview::firstName).setHeader("First name").setAutoWidth(true);
        grid.addColumn(StaffMemberOverview::lastName).setHeader("Last name").setAutoWidth(true);
        grid.addColumn(staffMember -> roleLabel(staffMember.role())).setHeader("Role").setAutoWidth(true);
        grid.addColumn(staffMember -> staffMember.active() ? "Active" : "Inactive").setHeader("Status")
                .setAutoWidth(true);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setAllRowsVisible(true);
        // Step 3: select an existing staff member
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                edit(event.getValue());
            }
        });
    }

    private void createEditor() {
        username.setMaxLength(StaffMemberService.MAX_USERNAME_LENGTH);
        username.setRequiredIndicatorVisible(true);
        firstName.setMaxLength(StaffMemberService.MAX_NAME_LENGTH);
        firstName.setRequiredIndicatorVisible(true);
        lastName.setMaxLength(StaffMemberService.MAX_NAME_LENGTH);
        lastName.setRequiredIndicatorVisible(true);
        role.setLabel("Role");
        // UC-021 BR-002: Exactly one role: Receptionist, Manager or Housekeeper.
        role.setItems(StaffRole.values());
        role.setItemLabelGenerator(StaffMembersView::roleLabel);
        role.setRequiredIndicatorVisible(true);
        password.setRevealButtonVisible(false);
        // The view validates the password itself, so clearing it after a refused save keeps it marked (A1)
        password.setManualValidation(true);
        password.setAutocomplete(Autocomplete.NEW_PASSWORD);

        // UC-021 A1: The username is required and at most 50 characters.
        binder.forField(username)
                .asRequired("Enter the username")
                .withValidator(value -> !value.isBlank(), "Enter the username")
                .withValidator(value -> value.strip().length() <= StaffMemberService.MAX_USERNAME_LENGTH,
                        "The username can have at most " + StaffMemberService.MAX_USERNAME_LENGTH + " characters")
                .bind(StaffMemberInput::getUsername, StaffMemberInput::setUsername);
        // UC-021 A1: The first name is required and at most 100 characters.
        binder.forField(firstName)
                .asRequired("Enter the first name")
                .withValidator(value -> !value.isBlank(), "Enter the first name")
                .withValidator(value -> value.strip().length() <= StaffMemberService.MAX_NAME_LENGTH,
                        "The first name can have at most " + StaffMemberService.MAX_NAME_LENGTH + " characters")
                .bind(StaffMemberInput::getFirstName, StaffMemberInput::setFirstName);
        // UC-021 A1: The last name is required and at most 100 characters.
        binder.forField(lastName)
                .asRequired("Enter the last name")
                .withValidator(value -> !value.isBlank(), "Enter the last name")
                .withValidator(value -> value.strip().length() <= StaffMemberService.MAX_NAME_LENGTH,
                        "The last name can have at most " + StaffMemberService.MAX_NAME_LENGTH + " characters")
                .bind(StaffMemberInput::getLastName, StaffMemberInput::setLastName);
        // UC-021 BR-002: Every staff member has exactly one role.
        binder.forField(role)
                .asRequired("Select the role")
                .bind(StaffMemberInput::getRole, StaffMemberInput::setRole);
        binder.forField(active).bind(StaffMemberInput::isActive, StaffMemberInput::setActive);

        FormLayout form = new FormLayout(username, role, firstName, lastName, password, active);
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("30rem", 2));

        // Step 5
        Button saveButton = new Button("Save", _ -> save());
        saveButton.addThemeVariants(ButtonVariant.PRIMARY);
        // A6: the manager leaves the staff member without saving
        Button discardButton = new Button("Discard", _ -> closeEditor());
        discardButton.addThemeVariants(ButtonVariant.TERTIARY);

        editor.setPadding(false);
        editor.add(editorTitle, form, new HorizontalLayout(saveButton, discardButton));
        editor.setVisible(false);
    }

    /**
     * Steps 3 and 4, A4.
     */
    private void edit(StaffMemberOverview staffMember) {
        message.removeAll();
        editedStaffMemberId = staffMember == null ? null : staffMember.id();
        input = staffMember == null ? StaffMemberInput.empty() : StaffMemberInput.of(staffMember);
        editorTitle.setText(staffMember == null ? "New staff member" : "Staff member " + staffMember.username());
        // Step 4: an initial password for a new staff member; A4: a new password for an existing one
        password.setLabel(staffMember == null ? "Initial password" : "New password");
        password.setRequiredIndicatorVisible(staffMember == null);
        password.setHelperText(staffMember == null
                ? "At least " + StaffMemberService.MIN_PASSWORD_LENGTH + " characters"
                : "Leave empty to keep the current password; at least " + StaffMemberService.MIN_PASSWORD_LENGTH
                + " characters");
        clearPassword();
        binder.setBean(input);
        editor.setVisible(true);
        username.focus();
    }

    /**
     * Steps 5 to 8, A1 to A3 and A5.
     */
    private void save() {
        message.removeAll();
        boolean valuesValid = binder.validate().isOk();
        boolean passwordValid = validatePassword();
        // A1: invalid values are marked; the entered values except the password are kept
        if (!valuesValid || !passwordValid) {
            clearPasswordValue();
            return;
        }
        StaffMemberData data = new StaffMemberData(input.getUsername(), input.getFirstName(), input.getLastName(),
                input.getRole(), input.isActive(), password.getValue());
        try {
            service.save(editedStaffMemberId, data, managerUsername);
        } catch (UsernameUsedException _) {
            // A2: continue at step 4
            clearPasswordValue();
            username.setErrorMessage(USERNAME_USED);
            username.setInvalid(true);
            showMessage(USERNAME_USED, true);
            return;
        } catch (OwnAccountChangeException _) {
            // A3: refused; another manager has to make the change
            clearPasswordValue();
            showMessage(OWN_ACCOUNT, true);
            return;
        }
        // Step 8: the updated list
        String savedUsername = data.username().strip();
        closeEditor();
        showMessage(SAVED.formatted(savedUsername), false);
    }

    // UC-021 BR-003: A password has at least 10 characters; a new staff member needs an initial password.
    private boolean validatePassword() {
        String value = password.getValue();
        if (value.isEmpty()) {
            if (editedStaffMemberId == null) {
                password.setErrorMessage("Enter the initial password");
                password.setInvalid(true);
                return false;
            }
            password.setInvalid(false);
            return true;
        }
        if (value.length() < StaffMemberService.MIN_PASSWORD_LENGTH) {
            password.setErrorMessage(PASSWORD_TOO_SHORT);
            password.setInvalid(true);
            return false;
        }
        if (!StaffMemberService.isValidPassword(value)) {
            password.setErrorMessage("The password is too long");
            password.setInvalid(true);
            return false;
        }
        password.setInvalid(false);
        return true;
    }

    /**
     * UC-021 BR-003: The password is never shown again; after a refused save it has to be entered anew.
     */
    private void clearPasswordValue() {
        password.clear();
    }

    private void clearPassword() {
        password.clear();
        password.setInvalid(false);
    }

    private void closeEditor() {
        editor.setVisible(false);
        editedStaffMemberId = null;
        clearPassword();
        binder.setBean(null);
        grid.asSingleSelect().clear();
        refreshGrid();
    }

    private void refreshGrid() {
        grid.setItems(service.staffMembers());
    }

    private void showMessage(String text, boolean error) {
        Paragraph paragraph = new Paragraph(text);
        paragraph.setId("staff-member-message");
        if (error) {
            paragraph.getElement().setAttribute("role", "alert");
            paragraph.addClassNames(LumoUtility.TextColor.ERROR);
        }
        message.removeAll();
        message.add(paragraph);
    }

    static String roleLabel(StaffRole role) {
        if (role == null) {
            return "";
        }
        return switch (role) {
            case RECEPTIONIST -> "Receptionist";
            case MANAGER -> "Manager";
            case HOUSEKEEPER -> "Housekeeper";
        };
    }

    private static class StaffMemberInput {
        private String username;
        private String firstName;
        private String lastName;
        private StaffRole role;
        private boolean active;

        static StaffMemberInput empty() {
            StaffMemberInput input = new StaffMemberInput();
            input.username = "";
            input.firstName = "";
            input.lastName = "";
            input.active = true;
            return input;
        }

        static StaffMemberInput of(StaffMemberOverview staffMember) {
            StaffMemberInput input = new StaffMemberInput();
            input.username = staffMember.username();
            input.firstName = staffMember.firstName();
            input.lastName = staffMember.lastName();
            input.role = staffMember.role();
            input.active = staffMember.active();
            return input;
        }

        String getUsername() {
            return username;
        }

        void setUsername(String username) {
            this.username = username;
        }

        String getFirstName() {
            return firstName;
        }

        void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        String getLastName() {
            return lastName;
        }

        void setLastName(String lastName) {
            this.lastName = lastName;
        }

        StaffRole getRole() {
            return role;
        }

        void setRole(StaffRole role) {
            this.role = role;
        }

        boolean isActive() {
            return active;
        }

        void setActive(boolean active) {
            this.active = active;
        }
    }
}
