package op.edu.ua.petbed.telegram.callback;

import op.edu.ua.petbed.common.exceptions.PetBedException;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CallbackIdTest {

    @Nested
    class Factory_methods {
        @Test
        void id_returns_correct_value() {
            var result = CallbackId.PROFILE;

            assertThat(result.id()).isEqualTo(110);
        }

        @Test
        void label_returns_correct_value() {
            var result = CallbackId.PROFILE;

            assertThat(result.label()).isEqualTo("Профіль");
        }
    }

    @Nested
    class Hierarchy {
        @Test
        void children_returns_direct_children_only() {
            var menu = CallbackId.MENU;
            var children = menu.children();

            assertThat(children)
                    .hasSize(6)
                    .contains(CallbackId.PROFILE, CallbackId.MY_PETS, CallbackId.FEED, CallbackId.SEARCH, CallbackId.FOSTER, CallbackId.ADOPTION);
        }

        @Test
        void children_of_leaf_has_no_children() {
            var petUpdate = CallbackId.PET_UPDATE;
            var children = petUpdate.children();

            assertThat(children).isEmpty();
        }

        @Test
        void parent_returns_correct_parent() {
            var profile = CallbackId.PROFILE;
            var myPets = CallbackId.MY_PETS;

            assertThat(profile.parent()).isEqualTo(CallbackId.MENU);
            assertThat(myPets.parent()).isEqualTo(CallbackId.MENU);
        }

        @Test
        void parent_of_root_is_null() {
            var menu = CallbackId.MENU;

            assertThat(menu.parent()).isNull();
        }
    }

    @Nested
    class Parsing {
        @Test
        void fromId_returns_correct_enum() {
            var result = CallbackId.fromId(110);

            assertThat(result).isEqualTo(CallbackId.PROFILE);
        }

        @Test
        void fromId_throws_for_unknown() {
            assertThatThrownBy(() -> CallbackId.fromId(999))
                    .isInstanceOf(PetBedException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PetBedException.ErrorCode.INVALID_CALLBACK);
        }
    }

    @Nested
    class Graphviz {
        @Test
        void toMermaidGraph_returns_valid_mermaid() {
            var result = CallbackId.toMermaidGraph();

            assertThat(result)
                    .startsWith("graph TD")
                    .contains("MENU[\"Меню\"]")
                    .contains("MENU --> PROFILE")
                    .contains("PROFILE[\"Профіль\"]");
        }
    }
}
