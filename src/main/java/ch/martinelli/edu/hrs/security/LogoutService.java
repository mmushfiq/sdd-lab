package ch.martinelli.edu.hrs.security;

import ch.martinelli.edu.hrs.security.ui.LoginView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Service;

/**
 * UC-023 Log Out: ends the staff member's session and displays the staff login.
 */
@Service
public class LogoutService {

    /**
     * Steps 2 and 3.
     */
    public void logOut() {
        UI ui = UI.getCurrent();
        // Step 3: the browser loads the staff login once the session is ended
        if (ui != null) {
            ui.getPage().setLocation("/" + LoginView.ROUTE);
        }
        // UC-023 BR-001: The HTTP session is invalidated, so going back shows no staff data.
        // UC-023 BR-002: Input that was not saved is discarded together with the session.
        VaadinServletRequest request = VaadinServletRequest.getCurrent();
        new SecurityContextLogoutHandler().logout(request == null ? null : request.getHttpServletRequest(), null,
                SecurityContextHolder.getContext().getAuthentication());
    }
}
