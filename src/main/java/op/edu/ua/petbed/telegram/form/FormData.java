package op.edu.ua.petbed.telegram.form;

import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.form.scheme.FormInput;
import op.edu.ua.petbed.telegram.form.scheme.FormStep;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.SequencedMap;

import static op.edu.ua.petbed.common.exceptions.PetBedException.ErrorCode.INTERNAL_ERROR;

@NullMarked
public record FormData(
        Long userId,
        Long chatId,
        CallbackId returnCallback,
        SequencedMap<FormStep, FormInput> answers,
        @Nullable Long entityId
) {
    public FormData(Long userId, Long chatId, CallbackId returnCallback, SequencedMap<FormStep, FormInput> answers) {
        this(userId, chatId, returnCallback, answers, null);
    }

    public Long entityIdOrThrow() {
        if (entityId == null) throw new PetBedException("Entity ID is required", INTERNAL_ERROR);
        return entityId;
    }

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

    public String choice(FormStep step) {
        if (answers.get(step) instanceof FormInput.Choice(String v)) return v;
        throw new PetBedException("Expected CHOICE at step: " + step.prompt(), INTERNAL_ERROR);
    }

    public int number(FormStep step) {
        if (answers.get(step) instanceof FormInput.Number(int v)) return v;
        throw new PetBedException("Expected NUMBER at step: " + step.prompt(), INTERNAL_ERROR);
    }

    public @Nullable String textOrNull(FormStep step) {
        if (answers.get(step) instanceof FormInput.Text(String v)) return v;
        return null;
    }

    public @Nullable String photoOrNull(FormStep step) {
        if (answers.get(step) instanceof FormInput.Photo(String id)) return id;
        return null;
    }

    public @Nullable String choiceOrNull(FormStep step) {
        if (answers.get(step) instanceof FormInput.Choice(String v)) return v;
        return null;
    }

    public @Nullable Integer numberOrNull(FormStep step) {
        if (answers.get(step) instanceof FormInput.Number(int v)) return v;
        return null;
    }
}
