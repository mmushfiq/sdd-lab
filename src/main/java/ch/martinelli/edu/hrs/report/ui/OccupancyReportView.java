package ch.martinelli.edu.hrs.report.ui;

import ch.martinelli.edu.hrs.staff.StaffRole;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

/**
 * UC-019 Occupancy Report: start screen of the manager after login (UC-022 step 5). The content follows with UC-019.
 */
@Route("occupancy-report")
@PageTitle("Occupancy Report")
// UC-022 BR-003: Only the manager can reach UC-019.
@RolesAllowed(StaffRole.MANAGER_ROLE)
public class OccupancyReportView extends VerticalLayout {

    public OccupancyReportView() {
        add(new H1("Occupancy Report"));
    }
}
