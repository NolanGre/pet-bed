package op.edu.ua.petbed.telegram.form.scheme;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FormStepTest {

    @Nested
    class Validate_non_blank_text {

        @Test
        void With_valid_text_true() {
            // given
            var step = FormStep.text("Name");
            FormInput input = new FormInput.Text("Барсик");

            // when
            boolean result = step.validate(input);

            // then
            assertThat(result).isTrue();
        }

        @Test
        void With_blank_false() {
            // given
            var step = FormStep.text("Name");
            FormInput input = new FormInput.Text("   ");

            // when
            boolean result = step.validate(input);

            // then
            assertThat(result).isFalse();
        }

        @Test
        void With_empty_false() {
            // given
            var step = FormStep.text("Name");
            FormInput input = new FormInput.Text("");

            // when
            boolean result = step.validate(input);

            // then
            assertThat(result).isFalse();
        }

        @Test
        void With_photo_type_false() {
            // given
            var step = FormStep.text("Name");
            FormInput input = new FormInput.Photo("file123");

            // when
            boolean result = step.validate(input);

            // then
            assertThat(result).isFalse();
        }

        @Test
        void With_location_type_false() {
            // given
            var step = FormStep.text("Name");
            FormInput input = new FormInput.Location(50.45, 30.52);

            // when
            boolean result = step.validate(input);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    class Validate_non_blank_photo {

        @Test
        void With_valid_photo_true() {
            // given
            var step = FormStep.photo("Photo");
            FormInput input = new FormInput.Photo("file123");

            // when
            boolean result = step.validate(input);

            // then
            assertThat(result).isTrue();
        }

        @Test
        void With_blank_false() {
            // given
            var step = FormStep.photo("Photo");
            FormInput input = new FormInput.Photo("");

            // when
            boolean result = step.validate(input);

            // then
            assertThat(result).isFalse();
        }

        @Test
        void With_text_type_false() {
            // given
            var step = FormStep.photo("Photo");
            FormInput input = new FormInput.Text("name");

            // when
            boolean result = step.validate(input);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    class Validate_any_location {

        @Test
        void With_location_true() {
            // given
            var step = FormStep.location("Location");
            FormInput input = new FormInput.Location(50.45, 30.52);

            // when
            boolean result = step.validate(input);

            // then
            assertThat(result).isTrue();
        }

        @Test
        void With_text_type_false() {
            // given
            var step = FormStep.location("Location");
            FormInput input = new FormInput.Text("50.45,30.52");

            // when
            boolean result = step.validate(input);

            // then
            assertThat(result).isFalse();
        }

        @Test
        void With_photo_type_false() {
            // given
            var step = FormStep.location("Location");
            FormInput input = new FormInput.Photo("file123");

            // when
            boolean result = step.validate(input);

            // then
            assertThat(result).isFalse();
        }
    }
}