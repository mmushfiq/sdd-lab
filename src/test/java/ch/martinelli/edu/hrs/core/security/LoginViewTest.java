package ch.martinelli.edu.hrs.core.security;

import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import com.vaadin.flow.component.login.LoginForm;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginViewTest extends HrsBrowserlessTest {

    @Test
    void loginFormShownToAnonymousUsers() {
        navigate(LoginView.class);

        assertThat($(LoginForm.class).single().isVisible()).isTrue();
    }
}
