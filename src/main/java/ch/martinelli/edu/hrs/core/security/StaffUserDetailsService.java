package ch.martinelli.edu.hrs.core.security;

import org.jooq.DSLContext;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import static ch.martinelli.edu.hrs.db.Tables.STAFF_USER;

@Service
public class StaffUserDetailsService implements UserDetailsService {

    private final DSLContext dsl;

    public StaffUserDetailsService(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        var staffUser = dsl.selectFrom(STAFF_USER)
                .where(STAFF_USER.USERNAME.eq(username))
                .fetchOptional()
                .orElseThrow(() -> new UsernameNotFoundException(username));
        return User.withUsername(staffUser.getUsername())
                .password(staffUser.getPasswordHash())
                .roles(staffUser.getRole())
                .disabled(!staffUser.getActive())
                .build();
    }
}
