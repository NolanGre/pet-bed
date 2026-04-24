package op.edu.ua.petbed.telegram.form;

import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.form.scheme.FormInput;
import op.edu.ua.petbed.telegram.form.scheme.FormStep;
import org.jspecify.annotations.NullMarked;

import java.util.SequencedMap;

import static op.edu.ua.petbed.common.exceptions.PetBedException.ErrorCode.INTERNAL_ERROR;

@NullMarked
public record FormData(
        Long userId,
        Long chatId,
        CallbackId returnCallback,
        SequencedMap<FormStep, FormInput> answers
) {
    public String text(FormStep step) {
        if (answers.get(step) instanceof FormInput.Text(String v)) return v;
        throw new PetBedException("Expected TEXT at step: " + step.prompt(), INTERNAL_ERROR);
    }

    public String photo(FormStep step) {
        if (answers.get(step) instanceof FormInput.Photo(String id)) return id;
        throw new PetBedException("Expected PHOTO at step: " + step.prompt(), INTERNAL_ERROR);
    }

    public FormInput.Location location(FormStep step) {
        if (answers.get(step) instanceof FormInput.Location loc) return loc;
        throw new PetBedException("Expected LOCATION at step: " + step.prompt(), INTERNAL_ERROR);
    }
}
