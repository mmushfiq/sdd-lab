package ch.martinelli.edu.hrs.security;

import ch.martinelli.edu.hrs.staff.StaffMemberRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Loads the active staff member who logs in (UC-022 step 3).
 */
@Service
public class StaffUserDetailsService implements UserDetailsService {

    private final StaffMemberRepository staffMemberRepository;

    public StaffUserDetailsService(StaffMemberRepository staffMemberRepository) {
        this.staffMemberRepository = staffMemberRepository;
    }

    // UC-022 BR-001: An inactive staff member is treated like an unknown username.
    @Override
    public UserDetails loadUserByUsername(String username) {
        return staffMemberRepository.findActiveAccount(username)
                .map(account -> User.withUsername(account.username())
                        .password(account.passwordHash())
                        // UC-022 BR-003: The session gets exactly the functions of the staff member's role.
                        .roles(account.role().name())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("No active staff member"));
    }
}
