package op.edu.ua.petbed.telegram.form.scheme;

import op.edu.ua.petbed.common.model.PetSex;
import op.edu.ua.petbed.common.model.PetSize;
import op.edu.ua.petbed.common.model.PetType;

import java.util.List;

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
    )),
    UPDATE_PET(List.of(
            FormStep.text("✏️ Введіть ім'я тварини").optional(),
            FormStep.photo("📷 Надішліть фото тварини").optional(),
            FormStep.text("🏷️ Введіть породу").optional(),
            FormStep.text("🎨 Введіть колір забарвлення").optional(),
            FormStep.text("✨ Введіть тип забарвлення").optional(),
            FormStep.number("🔢 Введіть кількість повних років").optional(),
            FormStep.choice("⚤ Оберіть стать", List.of(PetSex.values())).optional(),
            FormStep.choice("📏 Оберіть розмір", List.of(PetSize.values())).optional(),
            FormStep.text("📝 Введіть особливі прикмети").optional()
    )),
    SET_GEOLOCATION(List.of(
            FormStep.location("📍 Надішліть вашу геолокацію")
    )),
    CREATE_FEED_POST(List.of(
            FormStep.text("✏️ Введіть текст публікації"),
            FormStep.photo("📷 Надішліть фото"),
            FormStep.location("📍 Надішліть геолокацію")
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
