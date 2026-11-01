package ch.martinelli.edu.hrs.core.ui;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AccessDeniedException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.PageTitle;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Shown to a logged-in staff user who opens a view of another role; needs {@code @AccessDeniedErrorRouter} on that
 * view, otherwise Vaadin answers "not found".
 */
@PageTitle("Access denied")
public class AccessDeniedView extends VerticalLayout implements HasErrorParameter<AccessDeniedException> {

    public static final String TEXT = "Access denied";

    public AccessDeniedView() {
        add(new H1(TEXT));
    }

    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<AccessDeniedException> parameter) {
        return HttpServletResponse.SC_FORBIDDEN;
    }
}
