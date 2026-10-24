package ch.martinelli.edu.hrs.core;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables the scheduled runs of the system, such as the daily cleaning run (UC-015).
 */
@Configuration
@EnableScheduling
public class SchedulingConfiguration {
}
