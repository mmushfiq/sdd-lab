package ch.martinelli.edu.hrs.management;

/**
 * Another season already has the entered name (UC-025 A2, BR-001).
 */
public class SeasonNameUsedException extends RuntimeException {

    public SeasonNameUsedException(String name) {
        super("The name " + name + " is already used");
    }
}
