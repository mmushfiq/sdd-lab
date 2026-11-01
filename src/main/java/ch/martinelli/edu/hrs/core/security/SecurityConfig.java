package ch.martinelli.edu.hrs.core.security;

import ch.martinelli.edu.hrs.core.ui.LoginView;
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Views declare their access with {@code @AnonymousAllowed}, {@code @PermitAll} or {@code @RolesAllowed};
 * views without an annotation are denied.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Logout returns to the login page.
        http.with(VaadinSecurityConfigurer.vaadin(), vaadin -> vaadin
                .loginView(LoginView.class, "/" + LoginView.ROUTE)
                .defaultSuccessUrl("/staff"));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
