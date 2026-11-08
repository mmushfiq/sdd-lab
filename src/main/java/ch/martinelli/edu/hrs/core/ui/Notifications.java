package ch.martinelli.edu.hrs.core.ui;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;

public final class Notifications {

    private Notifications() {
    }

    public static void showError(String message) {
        Notification.show(message, 5000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
