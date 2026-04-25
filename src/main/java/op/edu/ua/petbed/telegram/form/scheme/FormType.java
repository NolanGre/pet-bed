package op.edu.ua.petbed.telegram.form.scheme;

import op.edu.ua.petbed.common.model.PetSex;
import op.edu.ua.petbed.common.model.PetSize;
import op.edu.ua.petbed.common.model.PetType;

import java.util.List;

import static op.edu.ua.petbed.telegram.form.scheme.FormStep.*;

/**
 * Defines available form types and their ordered steps.
 */
public enum FormType {
ADD_PET(List.of(
            FormStep.text("✏️ Введіть ім'я тварини"),
            FormStep.choice("🐾 Оберіть тип тварини", List.of(PetType.values())),
            FormStep.photo("📷 Надішліть фото тварини"),
            FormStep.text("🏷️ Введіть породу"),
            FormStep.text("🎨 Введіть колір забарвлення"),
            FormStep.text("✨ Введіть тип забарвлення"),
            FormStep.number("🔢 Введіть кількість повних років"),
            FormStep.choice("⚤ Оберіть стать", List.of(PetSex.values())),
            FormStep.choice("📏 Оберіть розмір", List.of(PetSize.values())),
            FormStep.text("📝 Введіть особливі прикмети")
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
