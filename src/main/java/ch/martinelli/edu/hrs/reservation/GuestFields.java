package ch.martinelli.edu.hrs.reservation;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;

/**
 * Name, email and phone of a guest with their validation, shared by the booking and change dialogs.
 */
public class GuestFields {

    private final TextField name = new TextField("Name");
    private final EmailField email = new EmailField("Email");
    private final TextField phone = new TextField("Phone");

    /**
     * @param binder a validation-only binder: values are read with {@link #value()}
     */
    public GuestFields(Binder<Object> binder) {
        bindText(binder, name, "Name", 100);
        // the field's built-in format check runs first in Binder, so it carries the message
        email.setI18n(new EmailField.EmailFieldI18n().setPatternErrorMessage("Enter a valid email address"));
        binder.forField(email)
                .asRequired("Email is required")
                .withValidator(v -> v.length() <= 254, "Email can have at most 254 characters")
                .bind(_ -> null, (_, _) -> {
                });
        bindText(binder, phone, "Phone", 30);
    }

    public Component[] components() {
        return new Component[]{name, email, phone};
    }

    public Guest value() {
        return new Guest(name.getValue().trim(), email.getValue().trim(), phone.getValue().trim());
    }

    void setValue(Guest guest) {
        name.setValue(guest.name());
        email.setValue(guest.email());
        phone.setValue(guest.phone());
    }

    private static void bindText(Binder<Object> binder, TextField field, String label, int maxLength) {
        binder.forField(field)
                .withValidator(v -> !v.isBlank(), label + " is required")
                .withValidator(v -> v.trim().length() <= maxLength,
                        "%s can have at most %d characters".formatted(label, maxLength))
                .bind(_ -> null, (_, _) -> {
                });
        field.setRequiredIndicatorVisible(true);
    }
}
