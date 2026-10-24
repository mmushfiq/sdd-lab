package ch.martinelli.edu.hrs.reception.ui;

import ch.martinelli.edu.hrs.staff.StaffRole;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

/**
 * UC-008 Arrivals and Departures: start screen of the receptionist after login (UC-022 step 5). The content follows with UC-008.
 */
@Route("arrivals-and-departures")
@PageTitle("Arrivals and Departures")
// UC-022 BR-003: Only the receptionist can reach UC-008.
@RolesAllowed(StaffRole.RECEPTIONIST_ROLE)
public class ArrivalsAndDeparturesView extends VerticalLayout {

    public ArrivalsAndDeparturesView() {
        add(new H1("Arrivals and Departures"));
    }
}
