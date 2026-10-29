package ch.martinelli.edu.hrs.reservation;

public class CapacityExceededException extends RuntimeException {

    public CapacityExceededException(int capacity) {
        super("This room allows at most %d guests".formatted(capacity));
    }
}
