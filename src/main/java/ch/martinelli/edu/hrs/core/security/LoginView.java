package ch.martinelli.edu.hrs.core.security;

import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.spring.security.AuthenticationContext;

@Route(value = LoginView.ROUTE, autoLayout = false)
@PageTitle("Login | Hotel Reservation System")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    public static final String ROUTE = "login";

    private final transient AuthenticationContext authenticationContext;
    private final LoginForm loginForm = new LoginForm();

    public LoginView(AuthenticationContext authenticationContext) {
        this.authenticationContext = authenticationContext;
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        loginForm.setAction(ROUTE);
        add(loginForm);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (authenticationContext.isAuthenticated()) {
            // The menu lists only views the user may open; ordered so the first one is the role's start view
            MenuConfiguration.getMenuEntries().stream().findFirst()
                    .ifPresent(entry -> event.forwardTo(entry.path().replaceFirst("^/", "")));
            return;
        }
        loginForm.setError(event.getLocation().getQueryParameters().getParameters().containsKey("error"));
    }
}
