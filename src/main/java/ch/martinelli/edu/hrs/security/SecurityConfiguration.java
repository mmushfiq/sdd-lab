package ch.martinelli.edu.hrs.security;

import ch.martinelli.edu.hrs.security.ui.LoginView;
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Staff login and role-based access (UC-022). Guest views are marked {@code @AnonymousAllowed}; staff views are
 * marked {@code @RolesAllowed} with the roles that may use them.
 */
@EnableWebSecurity
@Configuration
public class SecurityConfiguration {

    static final int BCRYPT_STRENGTH = 10;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // UC-023 step 3: after logging out the staff login is displayed
        http.with(VaadinSecurityConfigurer.vaadin(),
                configurer -> configurer.loginView(LoginView.class, "/" + LoginView.ROUTE));
        return http.build();
    }

    // UC-022 BR-004: Passwords are stored only as salted bcrypt hashes (cost factor 10).
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    }

    @Bean
    AuthenticationManager authenticationManager(StaffUserDetailsService userDetailsService,
                                                PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        // UC-022 BR-001: An unknown username gives the same answer as a wrong password.
        provider.setHideUserNotFoundExceptions(true);
        return new ProviderManager(provider);
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }
}
