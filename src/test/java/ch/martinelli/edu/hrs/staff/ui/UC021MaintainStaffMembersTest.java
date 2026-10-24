package ch.martinelli.edu.hrs.staff.ui;

import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import ch.martinelli.edu.hrs.db.tables.records.StaffMemberRecord;
import ch.martinelli.edu.hrs.housekeeping.ui.CleaningListView;
import ch.martinelli.edu.hrs.reception.ui.ArrivalsAndDeparturesView;
import ch.martinelli.edu.hrs.security.StaffSessionRegistry;
import ch.martinelli.edu.hrs.security.ui.LoginView;
import ch.martinelli.edu.hrs.staff.StaffMemberData;
import ch.martinelli.edu.hrs.staff.StaffMemberOverview;
import ch.martinelli.edu.hrs.staff.StaffMemberService;
import ch.martinelli.edu.hrs.staff.StaffRole;
import ch.martinelli.edu.hrs.usecase.UseCase;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.RouteNotFoundError;
import com.vaadin.flow.router.RouterLink;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithAnonymousUser;

import java.util.List;
import java.util.stream.IntStream;

import static ch.martinelli.edu.hrs.db.Tables.STAFF_MEMBER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Use case test for UC-021 "Maintain Staff Members". Test data: V119__uc021_test_data.sql (receptionist sam,
 * housekeeper hugo, inactive manager lea) and V103__uc022_test_data.sql (rita, mark, hanna, inactive ina); every
 * test staff member has the password Secret-123.
 */
@WithAnonymousUser
@ExtendWith(OutputCaptureExtension.class)
class UC021MaintainStaffMembersTest extends HrsBrowserlessTest {

    /** Rows created by the tests get sequence values below the IDs of the test data. */
    private static final long FIRST_TEST_DATA_ID = 1001;
    private static final long SAM = 3101;
    private static final long HUGO = 3102;
    private static final long MARK = 1002;
    private static final String TEST_PASSWORD = "Secret-123";

    @Autowired
    DSLContext ctx;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    StaffSessionRegistry staffSessionRegistry;

    @Autowired
    StaffMemberService staffMemberService;

    @AfterEach
    void restoreStaffMembers() {
        SecurityContextHolder.clearContext();
        ctx.deleteFrom(STAFF_MEMBER).where(STAFF_MEMBER.ID.lt(FIRST_TEST_DATA_ID)).execute();
        String hash = ctx.select(STAFF_MEMBER.PASSWORD_HASH).from(STAFF_MEMBER).where(STAFF_MEMBER.ID.eq(MARK))
                .fetchSingle(STAFF_MEMBER.PASSWORD_HASH);
        restore(SAM, "sam", "Sam", "Service", StaffRole.RECEPTIONIST, true, hash);
        restore(HUGO, "hugo", "Hugo", "Housekeeper", StaffRole.HOUSEKEEPER, true, hash);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Main success scenario
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-021", businessRules = {"BR-001", "BR-002", "BR-003"})
    void manager_creates_a_new_staff_member(CapturedOutput output) {
        logIn("mark", TEST_PASSWORD);

        // Step 1
        test($(RouterLink.class).withText("Staff members").single()).click();
        assertThat(getCurrentView()).isInstanceOf(StaffMembersView.class);

        // Step 2: username, first name, last name, role and whether active; no password
        Grid<StaffMemberOverview> grid = grid();
        assertThat(grid.getColumns()).extracting(Grid.Column::getHeaderText)
                .containsExactly("Username", "First name", "Last name", "Role", "Status");
        assertThat(row("mark")).containsExactly("mark", "Mark", "Manager", "Manager", "Active");
        assertThat(row("hanna")).containsExactly("hanna", "Hanna", "Housekeeping", "Housekeeper", "Active");
        assertThat(row("ina")).containsExactly("ina", "Ina", "Inactive", "Receptionist", "Inactive");
        assertThat(row("lea")).containsExactly("lea", "Lea", "Left", "Manager", "Inactive");

        // Step 3
        test($(Button.class).withText("New staff member").single()).click();
        // Step 4: BR-002 offers exactly the three roles
        assertThat(roleSelect().getListDataView().getItems()).containsExactly(StaffRole.RECEPTIONIST,
                StaffRole.MANAGER, StaffRole.HOUSEKEEPER);
        enter("nora", "Nora", "Neu", "Housekeeper");
        test(passwordField("Initial password")).setValue("Initial-Pass-1");
        // Step 5
        save();

        // Step 8: the updated list
        assertThat(message()).isEqualTo("Staff member nora is saved.");
        assertThat(row("nora")).containsExactly("nora", "Nora", "Neu", "Housekeeper", "Active");
        assertThat($(TextField.class).withCaption("Username").exists()).isFalse();

        // Step 7 / success postconditions: stored with a bcrypt hash only (BR-003), never logged
        StaffMemberRecord saved = ctx.selectFrom(STAFF_MEMBER).where(STAFF_MEMBER.USERNAME.eq("nora")).fetchSingle();
        assertThat(saved.getFirstName()).isEqualTo("Nora");
        assertThat(saved.getLastName()).isEqualTo("Neu");
        assertThat(saved.getRole()).isEqualTo("HOUSEKEEPER");
        assertThat(saved.getActive()).isTrue();
        assertThat(saved.getPasswordHash()).doesNotContain("Initial-Pass-1").startsWith("$2a$10$");
        assertThat(passwordEncoder.matches("Initial-Pass-1", saved.getPasswordHash())).isTrue();
        assertThat(output.getAll()).doesNotContain("Initial-Pass-1");

        // The new staff member can log in and uses exactly the functions of their role
        logOut();
        logIn("nora", "Initial-Pass-1");
        assertThat(getCurrentView()).isInstanceOf(CleaningListView.class);
        assertThat($(RouterLink.class).withText("Staff members").exists()).isFalse();
    }

    @Test
    @UseCase(id = "UC-021")
    void manager_changes_an_existing_staff_member() {
        openStaffMembers();

        // Step 3: select an existing staff member; the password is not shown
        select("sam");
        assertThat(username().getValue()).isEqualTo("sam");
        assertThat(firstName().getValue()).isEqualTo("Sam");
        assertThat(roleSelect().getValue()).isEqualTo(StaffRole.RECEPTIONIST);
        assertThat(passwordField("New password").getValue()).isEmpty();

        // Step 4
        test(lastName()).setValue("Smith");
        test(username()).setValue("samuel");
        save();

        assertThat(message()).isEqualTo("Staff member samuel is saved.");
        assertThat(row("samuel")).containsExactly("samuel", "Sam", "Smith", "Receptionist", "Active");
        // The password is kept when no new password is entered
        StaffMemberRecord saved = ctx.fetchSingle(STAFF_MEMBER, STAFF_MEMBER.ID.eq(SAM));
        assertThat(passwordEncoder.matches(TEST_PASSWORD, saved.getPasswordHash())).isTrue();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A1: Invalid Values
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-021", scenario = "A1: Invalid Values", businessRules = {"BR-002", "BR-003"})
    void missing_values_and_short_password_are_marked() {
        openStaffMembers();
        int staffMembers = ctx.fetchCount(STAFF_MEMBER);
        test($(Button.class).withText("New staff member").single()).click();
        test(firstName()).setValue("Nora");
        test(passwordField("Initial password")).setValue("short-pw1");

        save();

        // A1 step 1: the invalid values are marked; the entered values except the password are kept
        assertThat(username().isInvalid()).isTrue();
        assertThat(lastName().isInvalid()).isTrue();
        assertThat(roleSelect().isInvalid()).isTrue();
        assertThat(firstName().isInvalid()).isFalse();
        assertThat(firstName().getValue()).isEqualTo("Nora");
        PasswordField password = passwordField("Initial password");
        assertThat(password.isInvalid()).isTrue();
        assertThat(password.getErrorMessage()).isEqualTo("The password must have at least 10 characters");
        assertThat(password.getValue()).isEmpty();
        assertThat(ctx.fetchCount(STAFF_MEMBER)).isEqualTo(staffMembers);

        // A1 step 2: the use case continues at step 4
        enter("nora", "Nora", "Neu", "Receptionist");
        test(password).setValue("Long-enough");
        save();
        assertThat(message()).isEqualTo("Staff member nora is saved.");
    }

    @Test
    @UseCase(id = "UC-021", scenario = "A1: Invalid Values", businessRules = {"BR-003"})
    void new_staff_member_needs_an_initial_password() {
        openStaffMembers();
        test($(Button.class).withText("New staff member").single()).click();
        enter("nora", "Nora", "Neu", "Receptionist");

        save();

        assertThat(passwordField("Initial password").isInvalid()).isTrue();
        assertThat(passwordField("Initial password").getErrorMessage()).isEqualTo("Enter the initial password");
        assertThat(username().getValue()).isEqualTo("nora");
        assertThat(ctx.fetchExists(STAFF_MEMBER, STAFF_MEMBER.USERNAME.eq("nora"))).isFalse();
    }

    @Test
    @UseCase(id = "UC-021", scenario = "A1: Invalid Values")
    void values_longer_than_allowed_are_refused() {
        openStaffMembers();
        test($(Button.class).withText("New staff member").single()).click();

        // The fields accept at most the lengths of the entity model
        assertThat(username().getMaxLength()).isEqualTo(50);
        assertThat(firstName().getMaxLength()).isEqualTo(100);
        assertThat(lastName().getMaxLength()).isEqualTo(100);
        // The service refuses longer values as well
        assertThatThrownBy(() -> staffMemberService.save(null, new StaffMemberData("u".repeat(51), "Nora", "Neu",
                StaffRole.RECEPTIONIST, true, "Long-enough"), "mark"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> staffMemberService.save(null, new StaffMemberData("nora", "N".repeat(101), "Neu",
                StaffRole.RECEPTIONIST, true, "Long-enough"), "mark"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> staffMemberService.save(null, new StaffMemberData("nora", "Nora", "N".repeat(101),
                StaffRole.RECEPTIONIST, true, "Long-enough"), "mark"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(ctx.fetchExists(STAFF_MEMBER, STAFF_MEMBER.USERNAME.eq("nora"))).isFalse();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A2: Username Already Used
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-021", scenario = "A2: Username Already Used", businessRules = {"BR-001"})
    void username_used_by_another_staff_member_is_refused_case_insensitively() {
        openStaffMembers();
        int staffMembers = ctx.fetchCount(STAFF_MEMBER);
        test($(Button.class).withText("New staff member").single()).click();
        enter("RITA", "Rita", "Zweite", "Receptionist");
        test(passwordField("Initial password")).setValue("Long-enough");

        save();

        // A2 step 1
        assertThat(message()).isEqualTo("The username is already used by another staff member.");
        assertThat(username().isInvalid()).isTrue();
        assertThat(username().getValue()).isEqualTo("RITA");
        assertThat(ctx.fetchCount(STAFF_MEMBER)).isEqualTo(staffMembers);

        // A2 step 2: the use case continues at step 4
        test(username()).setValue("rita2");
        test(passwordField("Initial password")).setValue("Long-enough");
        save();
        assertThat(message()).isEqualTo("Staff member rita2 is saved.");
    }

    @Test
    @UseCase(id = "UC-021", scenario = "A2: Username Already Used", businessRules = {"BR-001"})
    void existing_staff_member_cannot_take_another_username() {
        openStaffMembers();
        select("sam");
        test(username()).setValue("Hugo");

        save();

        assertThat(message()).isEqualTo("The username is already used by another staff member.");
        assertThat(ctx.fetchSingle(STAFF_MEMBER, STAFF_MEMBER.ID.eq(SAM)).getUsername()).isEqualTo("sam");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A3: Manager Changes Their Own Account
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-021", scenario = "A3: Manager Changes Their Own Account", businessRules = {"BR-004"})
    void manager_cannot_deactivate_their_own_account() {
        openStaffMembers();
        select("mark");
        test($(Checkbox.class).withCaption("Active").single()).click();

        save();

        // A3 step 1
        assertThat(message()).isEqualTo("You cannot deactivate your own account or change your own role. "
                + "Another manager has to make this change.");
        assertThat(ctx.fetchSingle(STAFF_MEMBER, STAFF_MEMBER.ID.eq(MARK)).getActive()).isTrue();
        // Failure postcondition: at least one active manager remains
        assertThat(ctx.fetchCount(STAFF_MEMBER, STAFF_MEMBER.ROLE.eq("MANAGER").and(STAFF_MEMBER.ACTIVE.isTrue())))
                .isPositive();
        // A3 step 2: the use case continues at step 4
        assertThat(username().getValue()).isEqualTo("mark");
    }

    @Test
    @UseCase(id = "UC-021", scenario = "A3: Manager Changes Their Own Account", businessRules = {"BR-004"})
    void manager_cannot_change_their_own_role() {
        openStaffMembers();
        select("mark");
        test(roleSelect()).selectItem("Receptionist");

        save();

        assertThat(message()).isEqualTo("You cannot deactivate your own account or change your own role. "
                + "Another manager has to make this change.");
        assertThat(ctx.fetchSingle(STAFF_MEMBER, STAFF_MEMBER.ID.eq(MARK)).getRole()).isEqualTo("MANAGER");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A4: Manager Resets a Password
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-021", scenario = "A4: Manager Resets a Password", businessRules = {"BR-003"})
    void manager_resets_the_password_of_a_staff_member(CapturedOutput output) {
        openStaffMembers();
        select("sam");

        // A4 step 1
        test(passwordField("New password")).setValue("Reset-Pass-77");
        save();

        assertThat(message()).isEqualTo("Staff member sam is saved.");
        String hash = ctx.fetchSingle(STAFF_MEMBER, STAFF_MEMBER.ID.eq(SAM)).getPasswordHash();
        assertThat(passwordEncoder.matches("Reset-Pass-77", hash)).isTrue();
        assertThat(output.getAll()).doesNotContain("Reset-Pass-77");

        // Sam logs in with the new password only
        logOut();
        logIn("sam", TEST_PASSWORD);
        assertThat(getCurrentView()).isInstanceOf(LoginView.class);
        logIn("sam", "Reset-Pass-77");
        assertThat(getCurrentView()).isInstanceOf(ArrivalsAndDeparturesView.class);
    }

    @Test
    @UseCase(id = "UC-021", scenario = "A4: Manager Resets a Password", businessRules = {"BR-003"})
    void new_password_shorter_than_10_characters_is_refused() {
        openStaffMembers();
        select("sam");
        test(lastName()).setValue("Smith");
        test(passwordField("New password")).setValue("too-short");

        save();

        assertThat(passwordField("New password").isInvalid()).isTrue();
        assertThat(passwordField("New password").getValue()).isEmpty();
        assertThat(lastName().getValue()).isEqualTo("Smith");
        StaffMemberRecord sam = ctx.fetchSingle(STAFF_MEMBER, STAFF_MEMBER.ID.eq(SAM));
        assertThat(sam.getLastName()).isEqualTo("Service");
        assertThat(passwordEncoder.matches(TEST_PASSWORD, sam.getPasswordHash())).isTrue();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A5: Staff Member Deactivated or Role Changed
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-021", scenario = "A5: Staff Member Deactivated or Role Changed", businessRules = {"BR-005"})
    void deactivating_a_staff_member_ends_their_sessions() {
        MockHttpSession hugoSession = new MockHttpSession();
        staffSessionRegistry.register("hugo", hugoSession);
        MockHttpSession samSession = new MockHttpSession();
        staffSessionRegistry.register("sam", samSession);
        openStaffMembers();
        select("hugo");

        test($(Checkbox.class).withCaption("Active").single()).click();
        save();

        // A5 step 1: hugo's session is ended, sam's is not
        assertThat(hugoSession.isInvalid()).isTrue();
        assertThat(samSession.isInvalid()).isFalse();
        // A5 step 2 / BR-005: still listed, inactive, not deleted
        assertThat(row("hugo")).containsExactly("hugo", "Hugo", "Housekeeper", "Housekeeper", "Inactive");
        assertThat($(Button.class).withText("Delete").exists()).isFalse();

        // Success postcondition: an inactive staff member cannot log in
        logOut();
        logIn("hugo", TEST_PASSWORD);
        assertThat(getCurrentView()).isInstanceOf(LoginView.class);
        assertThat($(Paragraph.class).withId("error-message").single().getText())
                .isEqualTo("The username or password is wrong.");
    }

    @Test
    @UseCase(id = "UC-021", scenario = "A5: Staff Member Deactivated or Role Changed")
    void changing_the_role_ends_the_sessions_and_applies_at_next_login() {
        MockHttpSession samSession = new MockHttpSession();
        staffSessionRegistry.register("sam", samSession);
        openStaffMembers();
        select("sam");

        test(roleSelect()).selectItem("Housekeeper");
        save();

        assertThat(samSession.isInvalid()).isTrue();
        // The change applies at the next login
        logOut();
        logIn("sam", TEST_PASSWORD);
        assertThat(getCurrentView()).isInstanceOf(CleaningListView.class);
    }

    @Test
    @UseCase(id = "UC-021", scenario = "A5: Staff Member Deactivated or Role Changed")
    void changing_only_the_name_keeps_the_sessions() {
        MockHttpSession samSession = new MockHttpSession();
        staffSessionRegistry.register("sam", samSession);
        openStaffMembers();
        select("sam");

        test(firstName()).setValue("Samantha");
        save();

        assertThat(samSession.isInvalid()).isFalse();
        staffSessionRegistry.endSessions("sam");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A6: Manager Discards the Changes
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-021", scenario = "A6: Manager Discards the Changes")
    void discarded_changes_are_not_saved() {
        openStaffMembers();
        select("sam");
        test(lastName()).setValue("Smith");
        test($(Checkbox.class).withCaption("Active").single()).click();

        // A6 step 1
        test($(Button.class).withText("Discard").single()).click();

        assertThat($(TextField.class).withCaption("Username").exists()).isFalse();
        assertThat(row("sam")).containsExactly("sam", "Sam", "Service", "Receptionist", "Active");
        StaffMemberRecord sam = ctx.fetchSingle(STAFF_MEMBER, STAFF_MEMBER.ID.eq(SAM));
        assertThat(sam.getLastName()).isEqualTo("Service");
        assertThat(sam.getActive()).isTrue();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Preconditions
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-021")
    void other_roles_cannot_maintain_staff_members() {
        // Precondition: the manager is logged in (NFR-009)
        logIn("rita", TEST_PASSWORD);

        assertThat($(RouterLink.class).withText("Staff members").exists()).isFalse();
        assertThat(navigate("staff-members", RouteNotFoundError.class)).isNotNull();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------------------------

    private void restore(long id, String username, String firstName, String lastName, StaffRole role,
                         boolean active, String hash) {
        ctx.update(STAFF_MEMBER)
                .set(STAFF_MEMBER.USERNAME, username)
                .set(STAFF_MEMBER.FIRST_NAME, firstName)
                .set(STAFF_MEMBER.LAST_NAME, lastName)
                .set(STAFF_MEMBER.ROLE, role.name())
                .set(STAFF_MEMBER.ACTIVE, active)
                .set(STAFF_MEMBER.PASSWORD_HASH, hash)
                .where(STAFF_MEMBER.ID.eq(id))
                .execute();
    }

    private void logIn(String username, String password) {
        navigate(LoginView.class);
        test($(TextField.class).withCaption("Username").single()).setValue(username);
        test($(PasswordField.class).withCaption("Password").single()).setValue(password);
        test($(Button.class).withText("Log in").single()).click();
    }

    private void logOut() {
        SecurityContextHolder.clearContext();
    }

    private void openStaffMembers() {
        logIn("mark", TEST_PASSWORD);
        navigate(StaffMembersView.class);
    }

    private void enter(String username, String firstName, String lastName, String role) {
        test(username()).setValue(username);
        test(firstName()).setValue(firstName);
        test(lastName()).setValue(lastName);
        test(roleSelect()).selectItem(role);
    }

    private void save() {
        test($(Button.class).withText("Save").single()).click();
    }

    private void select(String username) {
        Grid<StaffMemberOverview> grid = grid();
        int index = IntStream.range(0, test(grid).size())
                .filter(i -> test(grid).getRow(i).username().equals(username))
                .findFirst().orElseThrow();
        test(grid).select(index);
    }

    private TextField username() {
        return $(TextField.class).withCaption("Username").single();
    }

    private TextField firstName() {
        return $(TextField.class).withCaption("First name").single();
    }

    private TextField lastName() {
        return $(TextField.class).withCaption("Last name").single();
    }

    @SuppressWarnings("unchecked")
    private Select<StaffRole> roleSelect() {
        return $(Select.class).withCaption("Role").single();
    }

    private PasswordField passwordField(String caption) {
        return $(PasswordField.class).withCaption(caption).single();
    }

    private String message() {
        return $(Paragraph.class).withId("staff-member-message").single().getText();
    }

    @SuppressWarnings("unchecked")
    private Grid<StaffMemberOverview> grid() {
        return $(Grid.class).withId("staff-members").single();
    }

    private List<String> row(String username) {
        Grid<StaffMemberOverview> grid = grid();
        return IntStream.range(0, test(grid).size())
                .filter(index -> test(grid).getRow(index).username().equals(username))
                .mapToObj(index -> IntStream.range(0, grid.getColumns().size())
                        .mapToObj(column -> test(grid).getCellText(index, column)).toList())
                .findFirst()
                .orElse(List.of());
    }
}
