package ch.martinelli.edu.hrs.staff.ui;

import ch.martinelli.edu.hrs.housekeeping.ui.CleaningListView;
import ch.martinelli.edu.hrs.reception.ui.ArrivalsAndDeparturesView;
import ch.martinelli.edu.hrs.report.ui.OccupancyReportView;
import ch.martinelli.edu.hrs.staff.StaffRole;
import com.vaadin.flow.component.Component;

/**
 * The start screen of each role (UC-022 step 5).
 */
public final class StartScreens {

    private StartScreens() {
    }

    public static Class<? extends Component> of(StaffRole role) {
        return switch (role) {
            case RECEPTIONIST -> ArrivalsAndDeparturesView.class;
            case HOUSEKEEPER -> CleaningListView.class;
            case MANAGER -> OccupancyReportView.class;
        };
    }
}
