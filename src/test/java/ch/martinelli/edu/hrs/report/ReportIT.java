package ch.martinelli.edu.hrs.report;

import ch.martinelli.edu.hrs.core.ui.PlaywrightIT;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.YearMonth;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class ReportIT extends PlaywrightIT {

    @Autowired
    private Clock clock;

    @Test
    void managerOpensTheCurrentMonthsReport() {
        page.navigate(url("login"));
        mopo.waitForConnectionToSettle();
        page.locator("input[name='username']").fill("manager");
        page.locator("input[name='password']").fill("manager");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Log in")).click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
        mopo.waitForConnectionToSettle();

        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Reports")).click();
        mopo.waitForConnectionToSettle();

        assertThat(page).hasURL(url("manager/reports"));
        assertThat(page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions()
                .setName(ReportView.label(YearMonth.now(clock))))).isVisible();
        // with no rooms in the test database occupancy is "–", so only the figures' format is checked
        assertThat(page.locator("#occupancy")).hasText(Pattern.compile("–|\\d+\\.\\d%"));
        assertThat(page.locator("#revenue")).hasText(Pattern.compile("\\d+\\.\\d{2}"));
    }

    private String url(String route) {
        return "http://localhost:" + localServerPort + "/" + route;
    }
}
