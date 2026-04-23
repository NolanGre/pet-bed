package op.edu.ua.petbed.telegram.form.scheme;

import op.edu.ua.petbed.common.exceptions.PetBedException;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FormInputTest {

    @Nested
    class Text_record {

        @Test
        void Value_is_accessible() {
            // given
            FormInput.Text text = new FormInput.Text("hello");

            // then
            assertThat(text.value()).isEqualTo("hello");
        }
    }

    @Nested
    class Photo_record {

        @Test
        void FileId_is_accessible() {
            // given
            FormInput.Photo photo = new FormInput.Photo("abc123");

            // then
            assertThat(photo.fileId()).isEqualTo("abc123");
        }
    }

    @Nested
    class Location_record {

        @Test
        void Coordinates_are_accessible() {
            // given
            FormInput.Location location = new FormInput.Location(50.45, 30.52);

            // then
            assertThat(location.latitude()).isEqualTo(50.45);
            assertThat(location.longitude()).isEqualTo(30.52);
        }
    }
}