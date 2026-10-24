package ch.martinelli.edu.hrs.management;

/**
 * The personal data of a guest the manager may erase (UC-024 step 3).
 */
public record ErasableGuest(Long id, String firstName, String lastName, String email, String phone) {
}
