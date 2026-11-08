package ch.martinelli.edu.hrs.core.security;

import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@EnableWebSecurity
@Configuration
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Logout returns to the login page; after login without a saved request,
        // LoginView forwards to the role's first view
        http.with(VaadinSecurityConfigurer.vaadin(), configurer -> configurer
                .loginView(LoginView.class, "/" + LoginView.ROUTE)
                .defaultSuccessUrl("/" + LoginView.ROUTE));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
