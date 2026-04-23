package op.edu.ua.petbed.telegram.form.scheme;

import org.jspecify.annotations.NullMarked;

import java.util.function.Predicate;

/**
 * Schema of a single form step: prompt text, expected input type-tag, and validator.
 */
@NullMarked
public record FormStep(
        String prompt,
        FormInput input,
        Predicate<FormInput> validator
) {

    public static final Predicate<FormInput> NON_BLANK_TEXT =
            i -> i instanceof FormInput.Text(String v) && !v.isBlank();

    public static final Predicate<FormInput> NON_BLANK_PHOTO =
            i -> i instanceof FormInput.Photo(String id) && !id.isBlank();

    public static final Predicate<FormInput> ANY_LOCATION =
            i -> i instanceof FormInput.Location;

    public boolean validate(FormInput input) {
        return validator.test(input);
    }
}
