package ch.martinelli.edu.hrs.housekeeping.ui;

import ch.martinelli.edu.hrs.staff.StaffRole;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

/**
 * UC-013 Cleaning List: start screen of the housekeeper after login (UC-022 step 5). The content follows with UC-013.
 */
@Route("cleaning-list")
@PageTitle("Cleaning List")
// UC-022 BR-003: Only the housekeeper can reach UC-013.
@RolesAllowed(StaffRole.HOUSEKEEPER_ROLE)
public class CleaningListView extends VerticalLayout {

    public CleaningListView() {
        add(new H1("Cleaning List"));
    }
}
