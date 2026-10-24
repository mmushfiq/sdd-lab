package ch.martinelli.edu.hrs.security;

import ch.martinelli.edu.hrs.staff.StaffRole;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

/**
 * UC-022 Log In: checks username and password and starts the staff member's session.
 */
@Service
public class LoginService {

    private static final String ROLE_PREFIX = "ROLE_";

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final StaffSessionRegistry staffSessionRegistry;

    public LoginService(AuthenticationManager authenticationManager,
                        SecurityContextRepository securityContextRepository,
                        StaffSessionRegistry staffSessionRegistry) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.staffSessionRegistry = staffSessionRegistry;
    }

    /**
     * Steps 3 and 4.
     *
     * @return the role of the logged-in staff member
     * @throws BadCredentialsException when no active staff member has this username or the password does not match
     *                                 (A2)
     */
    public StaffRole logIn(String username, String password) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(username, password));
        } catch (AuthenticationException e) {
            // UC-022 BR-001: Every refusal is reported the same way.
            throw new BadCredentialsException("Login refused");
        }

        HttpServletRequest request = VaadinServletRequest.getCurrent().getHttpServletRequest();
        HttpServletResponse response = VaadinServletResponse.getCurrent().getHttpServletResponse();
        // A new session ID on login protects against session fixation
        request.changeSessionId();

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        // UC-021 A5: The session is known, so it can be ended when the staff member is deactivated.
        staffSessionRegistry.register(authentication.getName(), request.getSession());

        return roleOf(authentication);
    }

    private static StaffRole roleOf(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority != null && authority.startsWith(ROLE_PREFIX))
                .map(authority -> StaffRole.valueOf(authority.substring(ROLE_PREFIX.length())))
                .findFirst()
                .orElseThrow();
    }
}
