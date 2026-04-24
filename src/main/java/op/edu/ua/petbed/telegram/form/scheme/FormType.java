package op.edu.ua.petbed.telegram.form.scheme;

import java.util.List;

import static op.edu.ua.petbed.telegram.form.scheme.FormStep.*;

/**
 * Defines available form types and their ordered steps.
 */
public enum FormType {
    ADD_PET(List.of(
            new FormStep("✏️ Введіть ім'я тварини", FormInput.TEXT, NON_BLANK_TEXT),
            new FormStep("🐾 Оберіть тип тварини", FormInput.TEXT, NON_BLANK_TEXT),
            new FormStep("📷 Надішліть фото тварини", FormInput.PHOTO, NON_BLANK_PHOTO),
            new FormStep("🏷️ Введіть породу", FormInput.TEXT, FormStep.NON_BLANK_TEXT),
            new FormStep("🎨 Введіть колір забарвлення", FormInput.TEXT, FormStep.NON_BLANK_TEXT),
            new FormStep("✨ Введіть тип забарвлення", FormInput.TEXT, FormStep.NON_BLANK_TEXT),
            new FormStep("🔢 Введіть кількість повних років", FormInput.TEXT, NON_BLANK_TEXT),
            new FormStep("⚤ Оберіть стать (male/female)", FormInput.TEXT, FormStep.NON_BLANK_TEXT),
            new FormStep("📏 Оберіть розмір (small/medium/large)", FormInput.TEXT, FormStep.NON_BLANK_TEXT),
            new FormStep("📝 Введіть особливі примітки", FormInput.TEXT, NON_BLANK_TEXT)
    ));

    private final List<FormStep> steps;

    FormType(List<FormStep> steps) {
        this.steps = List.copyOf(steps);
    }

    /**
     * Returns the ordered list of steps for this form.
     */
    public List<FormStep> steps() {
        return steps;
    }
}
