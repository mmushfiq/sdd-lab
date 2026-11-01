package ch.martinelli.edu.hrs.core.ui;

import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AccessDeniedErrorRouter;
import jakarta.annotation.security.RolesAllowed;

/**
 * Test-only stand-in for a Manager staff view.
 */
@Route("test/manager-only")
@RolesAllowed("MANAGER")
@AccessDeniedErrorRouter
public class ManagerOnlyTestView extends VerticalLayout {

    public static final String CONTENT = "Manager data";

    public ManagerOnlyTestView() {
        add(new Paragraph(CONTENT));
    }
}
