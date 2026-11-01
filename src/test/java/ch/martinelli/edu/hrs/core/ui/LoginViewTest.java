package ch.martinelli.edu.hrs.core.ui;

import com.vaadin.flow.component.login.LoginForm;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithAnonymousUser;

import static org.assertj.core.api.Assertions.assertThat;

@WithAnonymousUser
class LoginViewTest extends HrsBrowserlessTest {

    @Test
    void anonymous_visitor_sees_the_login_form_without_error() {
        navigate(LoginView.class);

        LoginForm form = $(LoginForm.class).single();
        assertThat(form.isError()).isFalse();
        assertThat(form.isForgotPasswordButtonVisible()).isFalse();
    }

    // Scenarios "Wrong password" and "Unknown username": Spring Security redirects both to login?error
    @Test
    void failed_login_shows_generic_error() {
        navigate("login?error", LoginView.class);

        LoginForm form = $(LoginForm.class).single();
        assertThat(form.isError()).isTrue();
        assertThat(String.valueOf(form.getElement().getPropertyRaw("i18n"))).contains("Invalid username or password");
    }
}
