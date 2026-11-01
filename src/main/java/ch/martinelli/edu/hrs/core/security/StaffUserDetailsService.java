package ch.martinelli.edu.hrs.core.security;

import org.jooq.DSLContext;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import static ch.martinelli.edu.hrs.db.Tables.STAFF_USER;

@Service
public class StaffUserDetailsService implements UserDetailsService {

    private final DSLContext ctx;

    public StaffUserDetailsService(DSLContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public StaffUser loadUserByUsername(String username) {
        return ctx.selectFrom(STAFF_USER)
                .where(STAFF_USER.USERNAME.eq(username))
                .fetchOptional(r -> new StaffUser(r.getUsername(), r.getPasswordHash(), r.getFullName(), r.getRole()))
                .orElseThrow(() -> new UsernameNotFoundException("Unknown staff user"));
    }
}
