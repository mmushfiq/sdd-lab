package ch.martinelli.edu.hrs.core.ui;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.Component;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Locale;

@SpringBootTest
public abstract class HrsBrowserlessTest extends SpringBrowserlessTest {

    @BeforeAll
    public static void initLocale() {
        Locale.setDefault(Locale.ENGLISH);
    }

    /**
     * True if the current user can open the view; access control either reroutes or throws when denied.
     */
    protected boolean opens(Class<? extends Component> view) {
        try {
            navigate(view);
        } catch (RuntimeException e) {
            return false;
        }
        return view.isInstance(getCurrentView());
    }
}
