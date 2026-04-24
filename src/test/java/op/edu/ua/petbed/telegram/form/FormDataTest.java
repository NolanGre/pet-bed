package op.edu.ua.petbed.telegram.form;

import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.form.scheme.FormInput;
import op.edu.ua.petbed.telegram.form.scheme.FormStep;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FormDataTest {

    // .text() --------------------------------------------------------------------

    @Nested
    class Text {

        @Test
        void text_correctType_returnsValue() {
            // given
            FormStep step = new FormStep("Name", FormInput.TEXT, s -> true);
            var answers = new LinkedHashMap<FormStep, FormInput>();
            answers.put(step, new FormInput.Text("Barsik"));
            FormData data = new FormData(1L, 123L, CallbackId.MY_PETS, answers);

            // when
            String result = data.text(step);

            // then
            assertThat(result).isEqualTo("Barsik");
        }

        @Test
        void text_wrongType_throwsPetBedException() {
            // given
            FormStep step = new FormStep("Name", FormInput.TEXT, s -> true);
            FormStep wrongStep = new FormStep("Photo", FormInput.PHOTO, s -> true);
            var answers = new LinkedHashMap<FormStep, FormInput>();
            answers.put(step, new FormInput.Text("Barsik"));
            answers.put(wrongStep, new FormInput.Photo("abc123"));
            FormData data = new FormData(1L, 123L, CallbackId.MY_PETS, answers);

            // when/then
            assertThatThrownBy(() -> data.text(wrongStep))
                    .isInstanceOf(PetBedException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PetBedException.ErrorCode.INTERNAL_ERROR);
        }
    }

    // .photo() -------------------------------------------------------------------

    @Nested
    class Photo {

        @Test
        void photo_correctType_returnsValue() {
            // given
            FormStep step = new FormStep("Photo", FormInput.PHOTO, s -> true);
            var answers = new LinkedHashMap<FormStep, FormInput>();
            answers.put(step, new FormInput.Photo("abc123"));
            FormData data = new FormData(1L, 123L, CallbackId.MY_PETS, answers);

            // when
            String result = data.photo(step);

            // then
            assertThat(result).isEqualTo("abc123");
        }

        @Test
        void photo_wrongType_throwsPetBedException() {
            // given
            FormStep step = new FormStep("Photo", FormInput.PHOTO, s -> true);
            FormStep wrongStep = new FormStep("Name", FormInput.TEXT, s -> true);
            var answers = new LinkedHashMap<FormStep, FormInput>();
            answers.put(step, new FormInput.Photo("abc123"));
            answers.put(wrongStep, new FormInput.Text("Barsik"));
            FormData data = new FormData(1L, 123L, CallbackId.MY_PETS, answers);

            // when/then
            assertThatThrownBy(() -> data.photo(wrongStep))
                    .isInstanceOf(PetBedException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PetBedException.ErrorCode.INTERNAL_ERROR);
        }
    }

    // .location() -----------------------------------------------------------------

    @Nested
    class Location {

        @Test
        void location_correctType_returnsValue() {
            // given
            FormStep step = new FormStep("Location", FormInput.LOCATION, s -> true);
            var answers = new LinkedHashMap<FormStep, FormInput>();
            answers.put(step, new FormInput.Location(50.45, 30.52));
            FormData data = new FormData(1L, 123L, CallbackId.MY_PETS, answers);

            // when
            FormInput.Location result = data.location(step);

            // then
            assertThat(result.latitude()).isEqualTo(50.45);
            assertThat(result.longitude()).isEqualTo(30.52);
        }

        @Test
        void location_wrongType_throwsPetBedException() {
            // given
            FormStep step = new FormStep("Location", FormInput.LOCATION, s -> true);
            FormStep wrongStep = new FormStep("Name", FormInput.TEXT, s -> true);
            var answers = new LinkedHashMap<FormStep, FormInput>();
            answers.put(step, new FormInput.Location(50.45, 30.52));
            answers.put(wrongStep, new FormInput.Text("Barsik"));
            FormData data = new FormData(1L, 123L, CallbackId.MY_PETS, answers);

            // when/then
            assertThatThrownBy(() -> data.location(wrongStep))
                    .isInstanceOf(PetBedException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PetBedException.ErrorCode.INTERNAL_ERROR);
        }
    }
}