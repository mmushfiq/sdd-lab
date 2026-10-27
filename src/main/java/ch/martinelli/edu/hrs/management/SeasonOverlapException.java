package ch.martinelli.edu.hrs.management;

/**
 * At least one night of the season already belongs to another season (UC-025 A3, BR-002).
 */
public class SeasonOverlapException extends RuntimeException {

    private final transient SeasonOverview otherSeason;

    public SeasonOverlapException(SeasonOverview otherSeason) {
        super("The season overlaps the season " + otherSeason.name());
        this.otherSeason = otherSeason;
    }

    public SeasonOverview otherSeason() {
        return otherSeason;
    }
}
