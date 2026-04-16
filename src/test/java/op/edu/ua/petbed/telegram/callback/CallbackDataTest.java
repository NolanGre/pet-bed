package op.edu.ua.petbed.telegram.callback;

import op.edu.ua.petbed.common.exceptions.PetBedException;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CallbackDataTest {

    @Nested
    class Parsing {
        @Test
        void from_validCallbackData_parsesCorrectly() {
            var result = CallbackData.from("123,42,10");

            assertThat(result.callbackId()).isEqualTo(123);
            assertThat(result.entityId()).isEqualTo(42L);
            assertThat(result.offset()).isEqualTo(10);
        }

        @Test
        void from_callbackIdOnly_parsesCorrectly() {
            var result = CallbackData.from("110,,5");

            assertThat(result.callbackId()).isEqualTo(110);
            assertThat(result.entityId()).isNull();
            assertThat(result.offset()).isEqualTo(5);
        }

        @Test
        void from_callbackIdAndEntity_parsesCorrectly() {
            var result = CallbackData.from("123,42,");

            assertThat(result.callbackId()).isEqualTo(123);
            assertThat(result.entityId()).isEqualTo(42L);
            assertThat(result.offset()).isNull();
        }

        @Test
        void from_emptyString_throws() {
            assertThatThrownBy(() -> CallbackData.from(""))
                    .isInstanceOf(PetBedException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PetBedException.ErrorCode.INVALID_CALLBACK);
        }

        @Test
        void from_null_throws() {
            assertThatThrownBy(() -> CallbackData.from(null))
                    .isInstanceOf(PetBedException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PetBedException.ErrorCode.INVALID_CALLBACK);
        }

        @Test
        void from_invalid_throws() {
            assertThatThrownBy(() -> CallbackData.from("abc"))
                    .isInstanceOf(NumberFormatException.class);
        }

        @Test
        void from_whitespaceOnly_throws() {
            assertThatThrownBy(() -> CallbackData.from("   "))
                    .isInstanceOf(PetBedException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PetBedException.ErrorCode.INVALID_CALLBACK);
        }
    }

    @Nested
    class Factory {
        @Test
        void of_createsWithCallbackId() {
            var result = CallbackData.of(CallbackId.PROFILE, null, null);

            assertThat(result.callbackId()).isEqualTo(110);
            assertThat(result.entityId()).isNull();
            assertThat(result.offset()).isNull();
        }

        @Test
        void of_withEntityAndOffset() {
            var result = CallbackData.of(CallbackId.PET_DETAIL, 42L, 5);

            assertThat(result.callbackId()).isEqualTo(123);
            assertThat(result.entityId()).isEqualTo(42L);
            assertThat(result.offset()).isEqualTo(5);
        }
    }

    @Nested
    class ToString {
        @Test
        void toString_withAllFields() {
            var data = new CallbackData(123, 42L, 10);

            assertThat(data.toString()).hasToString("123,42,10");
        }

        @Test
        void toString_withNullFields() {
            var data = new CallbackData(110, null, null);

            assertThat(data.toString()).hasToString("110,,");
        }

        @Test
        void toString_withEntityOnly() {
            var data = new CallbackData(123, 42L, null);

            assertThat(data.toString()).hasToString("123,42,");
        }
    }

    @Nested
    class CallbackIdEnum {
        @Test
        void callbackIdEnum_returnsCorrectEnum() {
            var data = CallbackData.from("110,,");

            var result = data.callbackIdEnum();

            assertThat(result).isEqualTo(CallbackId.PROFILE);
        }

        @Test
        void callbackIdEnum_throwsForUnknown() {
            var data = CallbackData.from("999,,");

            assertThatThrownBy(() -> data.callbackIdEnum())
                    .isInstanceOf(PetBedException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PetBedException.ErrorCode.INVALID_CALLBACK);
        }
    }
}
