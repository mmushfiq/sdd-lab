package ch.martinelli.edu.hrs.reception;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * UC-008 View Arrivals and Departures: today's arrivals and departures. Changes no data.
 */
@Service
public class ArrivalsAndDeparturesService {

    private final ArrivalsAndDeparturesRepository repository;
    private final Clock clock;

    public ArrivalsAndDeparturesService(ArrivalsAndDeparturesRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    // UC-008 BR-003: "Today" is the current date in the hotel's local time (the time zone of the clock).
    public LocalDate today() {
        return LocalDate.now(clock);
    }

    public List<Arrival> arrivals() {
        return repository.findArrivals(today());
    }

    public List<Departure> departures() {
        return repository.findDepartures(today());
    }
}
