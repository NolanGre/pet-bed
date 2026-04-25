package op.edu.ua.petbed.telegram.form.scheme;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

/**
 * Schema of a single form step: prompt text, expected input type-tag, and validator.
 */
@NullMarked
public record FormStep(
        String prompt,
        FormInput input,
        Predicate<FormInput> validator,
        @Nullable List<? extends Enum<?>> enumValues
) {

    public static final Predicate<FormInput> NON_BLANK_TEXT =
            i -> i instanceof FormInput.Text(String v) && !v.isBlank();

    public static final Predicate<FormInput> NON_BLANK_PHOTO =
            i -> i instanceof FormInput.Photo(String id) && !id.isBlank();

    public static final Predicate<FormInput> ANY_LOCATION =
            i -> i instanceof FormInput.Location;

    public static final Predicate<FormInput> POSITIVE_INTEGER =
            i -> i instanceof FormInput.Number(int v) && v > 0;

    public static final Predicate<FormInput> NON_NEGATIVE_INTEGER =
            i -> i instanceof FormInput.Number(int v) && v >= 0;


    public boolean validate(FormInput input) {
        return validator.test(input);
    }

    public boolean isChoice() {
        return this.input instanceof FormInput.Choice;
    }

    public static FormStep text(String prompt) {
        return new FormStep(prompt, FormInput.TEXT, NON_BLANK_TEXT, null);
    }

    public static FormStep photo(String prompt) {
        return new FormStep(prompt, FormInput.PHOTO, NON_BLANK_PHOTO, null);
    }

    public static FormStep location(String prompt) {
        return new FormStep(prompt, FormInput.LOCATION, ANY_LOCATION, null);
    }

    public static FormStep number(String prompt) {
        return new FormStep(prompt, FormInput.NUMBER, NON_NEGATIVE_INTEGER, null);
    }

    public static FormStep number(String prompt, Predicate<FormInput> validator) {
        return new FormStep(prompt, FormInput.NUMBER, validator, null);
    }

    public static <E extends Enum<E>> FormStep choice(String prompt, List<E> values) {
        return new FormStep(prompt, FormInput.CHOICE, i -> i instanceof FormInput.Choice, List.copyOf(values));
    }
}
