package ch.martinelli.edu.hrs.report;

/**
 * The entered period breaks UC-019 BR-004 (UC-019 A1).
 */
public class InvalidReportPeriodException extends RuntimeException {

    public InvalidReportPeriodException(String message) {
        super(message);
    }
}
