package ch.martinelli.edu.hrs.security;

import ch.martinelli.edu.hrs.staff.StaffSessions;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Knows the open HTTP sessions of every logged-in staff member, so that they can be ended when the manager
 * deactivates the staff member or changes their role (UC-021 A5).
 */
@Component
public class StaffSessionRegistry implements StaffSessions, HttpSessionListener {

    private final Map<String, Set<HttpSession>> sessions = new ConcurrentHashMap<>();

    /**
     * Records the session of a staff member who just logged in (UC-022 step 4).
     */
    public void register(String username, HttpSession session) {
        sessions.computeIfAbsent(key(username), _ -> ConcurrentHashMap.newKeySet()).add(session);
    }

    // UC-021 A5: Every open session of the staff member ends, so the change applies at their next login.
    @Override
    public void endSessions(String username) {
        Set<HttpSession> open = sessions.remove(key(username));
        if (open == null) {
            return;
        }
        for (HttpSession session : open) {
            try {
                session.invalidate();
            } catch (IllegalStateException _) {
                // already ended
            }
        }
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        sessions.values().forEach(open -> open.remove(event.getSession()));
    }

    private static String key(String username) {
        return username.toLowerCase(Locale.ROOT);
    }
}
