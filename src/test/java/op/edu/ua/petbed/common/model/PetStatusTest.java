package op.edu.ua.petbed.common.model;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PetStatusTest {

    @Nested
    class CanBeDeclaredLost {

        @Test
        void defaultStatus_returnsTrue() {
            assertThat(PetStatus.DEFAULT.canBeDeclaredLost()).isTrue();
        }

        @ParameterizedTest
        @CsvSource({
                "IN_LOST",
                "IN_ADOPTION",
                "IN_FOSTERING",
                "FOSTERED"
        })
        void nonDefaultStatuses_returnFalse(PetStatus status) {
            assertThat(status.canBeDeclaredLost()).isFalse();
        }
    }

    @Nested
    class CanUpdate {

        @Test
        void defaultStatus_returnsTrue() {
            assertThat(PetStatus.DEFAULT.canUpdate()).isTrue();
        }

        @ParameterizedTest
        @CsvSource({
                "IN_LOST",
                "IN_ADOPTION",
                "IN_FOSTERING",
                "FOSTERED"
        })
        void nonDefaultStatuses_returnFalse(PetStatus status) {
            assertThat(status.canUpdate()).isFalse();
        }
    }

    @Nested
    class CanDelete {

        @ParameterizedTest
        @CsvSource({
                "DEFAULT",
                "IN_LOST",
                "IN_ADOPTION",
                "IN_FOSTERING"
        })
        void nonFosteredStatuses_returnTrue(PetStatus status) {
            assertThat(status.canDelete()).isTrue();
        }

        @Test
        void fosteredStatus_returnsFalse() {
            assertThat(PetStatus.FOSTERED.canDelete()).isFalse();
        }
    }
}
