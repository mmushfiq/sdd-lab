package ch.martinelli.edu.hrs.core.ui;

import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route(LoginView.ROUTE)
@PageTitle("Staff Login")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    public static final String ROUTE = "login";
    public static final String ERROR_TEXT = "Invalid username or password";

    private final LoginForm loginForm = new LoginForm();

    public LoginView() {
        LoginI18n i18n = LoginI18n.createDefault();
        // Same text for unknown username and wrong password.
        i18n.getErrorMessage().setTitle(ERROR_TEXT);
        i18n.getErrorMessage().setMessage("");
        loginForm.setI18n(i18n);
        loginForm.setAction(ROUTE);
        loginForm.setForgotPasswordButtonVisible(false);

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        add(loginForm);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        loginForm.setError(event.getLocation().getQueryParameters().getParameters().containsKey("error"));
    }
}
