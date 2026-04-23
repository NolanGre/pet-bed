package op.edu.ua.petbed.telegram.form;

import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.form.scheme.FormInput;
import op.edu.ua.petbed.telegram.form.scheme.FormType;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FormEntityTest {

    @Nested
    class Initiate {

        @Test
        void Sets_all_fields() {
            // when
            FormEntity entity = FormEntity.initiate(123L, 456L, FormType.ADD_PET, CallbackId.MY_PETS);

            // then
            assertThat(entity.getTelegramId()).isEqualTo(123L);
            assertThat(entity.getChatId()).isEqualTo(456L);
            assertThat(entity.getFormType()).isEqualTo(FormType.ADD_PET);
            assertThat(entity.getReturnCallback()).isEqualTo(CallbackId.MY_PETS);
        }
    }

    @Nested
    class NextStep {

        @Test
        void Returns_first_step_when_empty() {
            // given
            FormEntity entity = FormEntity.initiate(123L, 456L, FormType.ADD_PET, CallbackId.MY_PETS);

            // when
            var step = entity.nextStep();

            // then
            assertThat(step.prompt()).contains("Введіть ім'я тварини");
        }

        @Test
        void Returns_next_unfilled_step() {
            // given
            FormEntity entity = FormEntity.initiate(123L, 456L, FormType.ADD_PET, CallbackId.MY_PETS);
            entity.applyStep(new FormInput.Text("Барсик"));

            // when
            var step = entity.nextStep();

            // then
            assertThat(step.prompt()).contains("Надішліть фото");
        }

        @Test
        void When_complete_throws_PetBedException() {
            // given
            FormEntity entity = FormEntity.initiate(123L, 456L, FormType.ADD_PET, CallbackId.MY_PETS);
            entity.applyStep(new FormInput.Text("Барсик"));
            entity.applyStep(new FormInput.Photo("file123"));
            entity.applyStep(new FormInput.Location(50.45, 30.52));

            // when & then
            assertThatThrownBy(() -> entity.nextStep())
                    .isInstanceOf(PetBedException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PetBedException.ErrorCode.INTERNAL_ERROR);
        }
    }

    @Nested
    class IsComplete {

        @Test
        void All_steps_filled_true() {
            // given
            FormEntity entity = FormEntity.initiate(123L, 456L, FormType.ADD_PET, CallbackId.MY_PETS);
            entity.applyStep(new FormInput.Text("Барсик"));
            entity.applyStep(new FormInput.Photo("file123"));
            entity.applyStep(new FormInput.Location(50.45, 30.52));

            // when
            boolean result = entity.isComplete();

            // then
            assertThat(result).isTrue();
        }

        @Test
        void Not_all_filled_false() {
            // given
            FormEntity entity = FormEntity.initiate(123L, 456L, FormType.ADD_PET, CallbackId.MY_PETS);
            entity.applyStep(new FormInput.Text("Барсик"));

            // when
            boolean result = entity.isComplete();

            // then
            assertThat(result).isFalse();
        }

        @Test
        void Empty_form_false() {
            // given
            FormEntity entity = FormEntity.initiate(123L, 456L, FormType.ADD_PET, CallbackId.MY_PETS);

            // when
            boolean result = entity.isComplete();

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    class ApplyStep {

        @Test
        void Saves_text_input() {
            // given
            FormEntity entity = FormEntity.initiate(123L, 456L, FormType.ADD_PET, CallbackId.MY_PETS);

            // when
            entity.applyStep(new FormInput.Text("Барсик"));

            // then
            // Verify by calling nextStep and checking we moved to next step
            var step = entity.nextStep();
            assertThat(step.prompt()).contains("Надішліть фото");
        }

        @Test
        void Saves_photo_input() {
            // given
            FormEntity entity = FormEntity.initiate(123L, 456L, FormType.ADD_PET, CallbackId.MY_PETS);
            entity.applyStep(new FormInput.Text("Барсик"));

            // when
            entity.applyStep(new FormInput.Photo("file123"));

            // then
            var step = entity.nextStep();
            assertThat(step.prompt()).contains("локацію");
        }

        @Test
        void Saves_location_input() {
            // given
            FormEntity entity = FormEntity.initiate(123L, 456L, FormType.ADD_PET, CallbackId.MY_PETS);
            entity.applyStep(new FormInput.Text("Барсик"));
            entity.applyStep(new FormInput.Photo("file123"));

            // when
            entity.applyStep(new FormInput.Location(50.45, 30.52));

            // then
            assertThat(entity.isComplete()).isTrue();
        }
    }
}