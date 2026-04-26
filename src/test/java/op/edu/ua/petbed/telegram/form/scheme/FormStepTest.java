package op.edu.ua.petbed.telegram.form.scheme;

import op.edu.ua.petbed.common.model.PetSex;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

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

    @Nested
    class IsChoice {

        @Test
        void choice_step_returns_true() {
            // given
            var step = FormStep.choice("Оберіть стать", List.of(PetSex.values()));

            // when
            boolean result = step.isChoice();

            // then
            assertThat(result).isTrue();
        }

        @Test
        void text_step_returns_false() {
            // given
            var step = FormStep.text("Ім'я");

            // when
            boolean result = step.isChoice();

            // then
            assertThat(result).isFalse();
        }

        @Test
        void photo_step_returns_false() {
            // given
            var step = FormStep.photo("Фото");

            // when
            boolean result = step.isChoice();

            // then
            assertThat(result).isFalse();
        }

        @Test
        void number_step_returns_false() {
            // given
            var step = FormStep.number("Вік");

            // when
            boolean result = step.isChoice();

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    class CanSkip {

        @Test
        void choice_step_returns_false() {
            // given
            var step = FormStep.choice("Оберіть стать", List.of(PetSex.values()));

            // when
            boolean result = step.canSkip();

            // then
            assertThat(result).isFalse();
        }

        @Test
        void text_step_returns_false_by_default() {
            // given
            var step = FormStep.text("Ім'я");

            // when
            boolean result = step.canSkip();

            // then
            assertThat(result).isFalse();
        }

        @Test
        void optional_text_step_returns_true() {
            // given
            var step = FormStep.text("Ім'я").optional();

            // when
            boolean result = step.canSkip();

            // then
            assertThat(result).isTrue();
        }

        @Test
        void photo_step_returns_false_by_default() {
            // given
            var step = FormStep.photo("Фото");

            // when
            boolean result = step.canSkip();

            // then
            assertThat(result).isFalse();
        }

        @Test
        void optional_photo_step_returns_true() {
            // given
            var step = FormStep.photo("Фото").optional();

            // when
            boolean result = step.canSkip();

            // then
            assertThat(result).isTrue();
        }

        @Test
        void number_step_returns_false_by_default() {
            // given
            var step = FormStep.number("Вік");

            // when
            boolean result = step.canSkip();

            // then
            assertThat(result).isFalse();
        }

        @Test
        void optional_number_step_returns_true() {
            // given
            var step = FormStep.number("Вік").optional();

            // when
            boolean result = step.canSkip();

            // then
            assertThat(result).isTrue();
        }

        @Test
        void location_step_returns_false_by_default() {
            // given
            var step = FormStep.location("Локація");

            // when
            boolean result = step.canSkip();

            // then
            assertThat(result).isFalse();
        }

        @Test
        void optional_location_step_returns_true() {
            // given
            var step = FormStep.location("Локація").optional();

            // when
            boolean result = step.canSkip();

            // then
            assertThat(result).isTrue();
        }
    }
}