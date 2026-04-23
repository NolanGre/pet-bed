package op.edu.ua.petbed.telegram.form.scheme;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FormTypeTest {

    @Nested
    class AddPet_steps {

        @Test
        void steps_count_is_3() {
            // when
            var steps = FormType.ADD_PET.steps();

            // then
            assertThat(steps).hasSize(3);
        }

        @Test
        void step_0_is_name_text_input() {
            // when
            var step = FormType.ADD_PET.steps().get(0);

            // then
            assertThat(step.prompt()).contains("Введіть ім'я тварини");
            // Check input type is Text (check record type)
            assertThat(step.input()).isInstanceOf(FormInput.Text.class);
        }

        @Test
        void step_1_is_photo_input() {
            // when
            var step = FormType.ADD_PET.steps().get(1);

            // then
            assertThat(step.prompt()).contains("Надішліть фото");
            assertThat(step.input()).isInstanceOf(FormInput.Photo.class);
        }

        @Test
        void step_2_is_location_input() {
            // when
            var step = FormType.ADD_PET.steps().get(2);

            // then
            assertThat(step.prompt()).contains("локацію");
            assertThat(step.input()).isInstanceOf(FormInput.Location.class);
        }
    }
}