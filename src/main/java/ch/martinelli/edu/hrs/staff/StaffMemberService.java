package ch.martinelli.edu.hrs.staff;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * UC-021 Maintain Staff Members: the manager creates, edits and deactivates staff members with their role.
 * Staff members are never deleted (UC-021 BR-005).
 */
@Service
public class StaffMemberService {

    public static final int MAX_USERNAME_LENGTH = 50;
    public static final int MAX_NAME_LENGTH = 100;
    public static final int MIN_PASSWORD_LENGTH = 10;
    /** bcrypt uses at most 72 bytes of a password. */
    public static final int MAX_PASSWORD_BYTES = 72;

    private final StaffMemberRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final StaffSessions staffSessions;
    private final TransactionTemplate transactionTemplate;

    public StaffMemberService(StaffMemberRepository repository, PasswordEncoder passwordEncoder,
                              StaffSessions staffSessions, TransactionTemplate transactionTemplate) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.staffSessions = staffSessions;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * Step 2.
     */
    public List<StaffMemberOverview> staffMembers() {
        return repository.findAll();
    }

    /**
     * Steps 6 and 7, A5: checks the values and records a new staff member ({@code staffMemberId} null) or changes
     * one.
     *
     * @param managerUsername the username of the logged-in manager (A3)
     * @throws IllegalArgumentException  when a value is invalid (A1)
     * @throws UsernameUsedException     when another staff member has the username (A2)
     * @throws OwnAccountChangeException when the manager deactivates their own account or changes their own role
     *                                   (A3)
     */
    public Long save(Long staffMemberId, StaffMemberData entered, String managerUsername) {
        StaffMemberData data = checkValues(entered, staffMemberId == null);
        // UC-021 BR-003: Only a salted bcrypt hash of the password is stored.
        String passwordHash = data.password() == null ? null : passwordEncoder.encode(data.password());
        Saved saved;
        try {
            saved = transactionTemplate.execute(_ -> {
                // UC-021 BR-001: Every staff member has a username no other staff member has (case-insensitive).
                if (repository.existsOtherWithUsername(data.username(), staffMemberId)) {
                    throw new UsernameUsedException(data.username());
                }
                if (staffMemberId == null) {
                    return new Saved(null, repository.insert(data, passwordHash));
                }
                StaffMemberOverview existing = repository.findForUpdate(staffMemberId)
                        .orElseThrow(() -> new IllegalArgumentException("Unknown staff member " + staffMemberId));
                // UC-021 BR-004: A manager cannot deactivate their own account or change their own role.
                if (existing.username().equalsIgnoreCase(managerUsername)
                        && (!data.active() || data.role() != existing.role())) {
                    throw new OwnAccountChangeException();
                }
                // UC-021 BR-005: Leaving staff members are deactivated, never deleted.
                repository.update(staffMemberId, data, passwordHash);
                return new Saved(existing, staffMemberId);
            });
        } catch (DuplicateKeyException _) {
            // UC-021 BR-001: another manager saved the same username at the same time
            throw new UsernameUsedException(data.username());
        }
        // UC-021 A5: Deactivation or a role change ends every open session of the staff member.
        StaffMemberOverview previous = saved.previous();
        if (previous != null && (previous.active() && !data.active() || previous.role() != data.role())) {
            staffSessions.endSessions(previous.username());
        }
        return saved.id();
    }

    // UC-021 A1: username, first and last name and role are required and not too long; a new staff member needs
    // an initial password of at least 10 characters.
    static StaffMemberData checkValues(StaffMemberData data, boolean newStaffMember) {
        String username = strip(data.username());
        String firstName = strip(data.firstName());
        String lastName = strip(data.lastName());
        if (username.isEmpty() || username.length() > MAX_USERNAME_LENGTH) {
            throw new IllegalArgumentException("The username must have 1 to " + MAX_USERNAME_LENGTH + " characters");
        }
        if (firstName.isEmpty() || firstName.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("The first name must have 1 to " + MAX_NAME_LENGTH + " characters");
        }
        if (lastName.isEmpty() || lastName.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("The last name must have 1 to " + MAX_NAME_LENGTH + " characters");
        }
        // UC-021 BR-002: Every staff member has exactly one role.
        if (data.role() == null) {
            throw new IllegalArgumentException("Select the role");
        }
        String password = data.password() == null || data.password().isEmpty() ? null : data.password();
        if (newStaffMember && password == null) {
            throw new IllegalArgumentException("Enter the initial password");
        }
        if (password != null && !isValidPassword(password)) {
            throw new IllegalArgumentException("The password must have at least " + MIN_PASSWORD_LENGTH
                    + " characters");
        }
        return new StaffMemberData(username, firstName, lastName, data.role(), data.active(), password);
    }

    // UC-021 BR-003: A password has at least 10 characters.
    public static boolean isValidPassword(String password) {
        return password.length() >= MIN_PASSWORD_LENGTH
                && password.getBytes(StandardCharsets.UTF_8).length <= MAX_PASSWORD_BYTES;
    }

    private static String strip(String value) {
        return value == null ? "" : value.strip();
    }

    private record Saved(StaffMemberOverview previous, Long id) {
    }
}
