package ch.martinelli.edu.hrs.room;

import ch.martinelli.edu.hrs.core.ui.PlaywrightIT;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static ch.martinelli.edu.hrs.db.Tables.ROOM_TYPE;
import static ch.martinelli.edu.hrs.db.Tables.STAFF_USER;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class ManagerIT extends PlaywrightIT {

    @Autowired
    private DSLContext dsl;

    @AfterEach
    void cleanUp() {
        dsl.deleteFrom(ROOM).where(ROOM.ROOM_NUMBER.eq("IT-101")).execute();
        dsl.deleteFrom(ROOM_TYPE).where(ROOM_TYPE.NAME.eq("IT Double")).execute();
        dsl.deleteFrom(STAFF_USER).where(STAFF_USER.USERNAME.eq("it-anna")).execute();
    }

    @Test
    void managerSetsUpRoomsAndStaff() {
        logIn("manager", "manager");
        assertThat(page).hasURL(url("manager/rooms"));

        open("manager/room-types");
        page.getByLabel("Name").fill("IT Double");
        page.getByLabel("Capacity").fill("2");
        page.getByLabel("Standard nightly rate").fill("120.00");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save")).click();
        assertThat(page.getByText("IT Double")).isVisible();

        open("manager/rooms");
        page.getByLabel("Room number").fill("IT-101");
        page.getByLabel("Room type").fill("IT Double");
        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName("IT Double")).click();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save")).click();
        assertThat(page.getByText("IT-101")).isVisible();

        open("manager/staff");
        page.getByLabel("Username").fill("it-anna");
        page.getByLabel("Role").click();
        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName("Receptionist")).click();
        page.getByLabel("Password", new Page.GetByLabelOptions().setExact(true)).fill("secret123");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Create")).click();
        assertThat(page.getByText("it-anna")).isVisible();

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Log out")).click();
        mopo.waitForConnectionToSettle();

        // The Receptionist lands on their first menu entry and cannot open manager views
        logIn("it-anna", "secret123");
        assertThat(page).hasURL(url("reception/front-desk"));
        assertThat(page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Rooms"))).hasCount(0);
        open("manager/rooms");
        assertThat(page.getByText("IT-101")).hasCount(0);
    }

    private void logIn(String username, String password) {
        open("login");
        page.locator("input[name='username']").fill(username);
        page.locator("input[name='password']").fill(password);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Log in")).click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
        mopo.waitForConnectionToSettle();
    }

    private void open(String route) {
        page.navigate(url(route));
        mopo.waitForConnectionToSettle();
    }

    private String url(String route) {
        return "http://localhost:" + localServerPort + "/" + route;
    }
}
