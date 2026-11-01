package ch.martinelli.edu.hrs.core.ui;

import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Test-only stand-in for a public guest view.
 */
@Route("test/guest")
@AnonymousAllowed
public class GuestTestView extends VerticalLayout {

    public static final String CONTENT = "Guest page";

    public GuestTestView() {
        add(new Paragraph(CONTENT));
    }
}
