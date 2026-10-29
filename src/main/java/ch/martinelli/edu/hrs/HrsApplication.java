package ch.martinelli.edu.hrs;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.lumo.Lumo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.Clock;
import java.time.ZoneId;

@StyleSheet(Lumo.STYLESHEET)
@StyleSheet(Lumo.UTILITY_STYLESHEET)
@StyleSheet("styles.css")
@SpringBootApplication
public class HrsApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(HrsApplication.class, args);
    }

    /**
     * Hotel time: "today" and the online cancellation deadline are in Europe/Zurich.
     */
    @Bean
    Clock clock() {
        return Clock.system(ZoneId.of("Europe/Zurich"));
    }
}
