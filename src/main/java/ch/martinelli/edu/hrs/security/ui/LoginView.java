package ch.martinelli.edu.hrs.security.ui;

import ch.martinelli.edu.hrs.security.LoginService;
import ch.martinelli.edu.hrs.staff.StaffRole;
import ch.martinelli.edu.hrs.staff.ui.StartScreens;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.Autocomplete;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.springframework.security.authentication.BadCredentialsException;

/**
 * UC-022 Log In: the staff member logs in with username and password and gets the start screen of their role.
 */
@Route(LoginView.ROUTE)
@PageTitle("Staff Login")
@AnonymousAllowed
public class LoginView extends VerticalLayout {

    public static final String ROUTE = "login";
    static final String LOGIN_REFUSED = "The username or password is wrong.";

    private final LoginService loginService;

    private final TextField username = new TextField("Username");
    private final PasswordField password = new PasswordField("Password");
    private final Button logInButton = new Button("Log in");
    private final Binder<Credentials> binder = new Binder<>();
    private final Credentials credentials = new Credentials();
    private final Div message = new Div();

    public LoginView(LoginService loginService) {
        this.loginService = loginService;

        setMaxWidth("24rem");
        getStyle().set("margin", "0 auto");

        username.setAutocomplete(Autocomplete.USERNAME);
        username.setRequiredIndicatorVisible(true);
        username.setWidthFull();
        username.setMaxLength(50);
        // UC-022 BR-004: The password is never shown.
        password.setRevealButtonVisible(false);
        password.setAutocomplete(Autocomplete.CURRENT_PASSWORD);
        password.setRequiredIndicatorVisible(true);
        password.setWidthFull();

        // A1: missing input is marked
        binder.forField(username)
                .asRequired("Enter your username")
                .bind(Credentials::getUsername, Credentials::setUsername);
        binder.forField(password)
                .asRequired("Enter your password")
                .bind(Credentials::getPassword, Credentials::setPassword);
        binder.setBean(credentials);

        logInButton.addThemeVariants(ButtonVariant.PRIMARY);
        logInButton.addClickListener(_ -> logIn());
        logInButton.addClickShortcut(Key.ENTER);

        message.setWidthFull();
        message.getElement().setAttribute("aria-live", "polite");

        add(new H1("Staff Login"), username, password, message, logInButton);
    }

    /**
     * Steps 3 to 5 and A2.
     */
    private void logIn() {
        message.removeAll();
        if (!binder.validate().isOk()) {
            return;
        }
        try {
            StaffRole role = loginService.logIn(credentials.getUsername(), credentials.getPassword());
            getUI().ifPresent(ui -> ui.navigate(StartScreens.of(role)));
        } catch (BadCredentialsException _) {
            // UC-022 BR-001: Same answer for unknown username, wrong password and inactive staff member.
            Paragraph refused = new Paragraph(LOGIN_REFUSED);
            refused.setId("error-message");
            refused.getElement().setAttribute("role", "alert");
            refused.addClassNames(LumoUtility.TextColor.ERROR);
            message.add(refused);
            password.focus();
        }
    }

    private static class Credentials {
        private String username;
        private String password;

        String getUsername() {
            return username;
        }

        void setUsername(String username) {
            this.username = username;
        }

        String getPassword() {
            return password;
        }

        void setPassword(String password) {
            this.password = password;
        }
    }
}
