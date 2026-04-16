package op.edu.ua.petbed.telegram.callback;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
    class Navigation {
        @Test
        void hasParent_returns_true_for_non_root() {
            assertThat(CallbackId.PROFILE.hasParent()).isTrue();
            assertThat(CallbackId.MENU.hasParent()).isFalse();
        }

        @Test
        void backButtonLabel_returns_back_with_label() {
            var profile = CallbackId.PROFILE;

            assertThat(profile.backButtonLabel()).isEqualTo("⬅️ Меню");
        }

        @Test
        void root_has_default_back_label() {
            var menu = CallbackId.MENU;

            assertThat(menu.backButtonLabel()).isEqualTo("⬅️ Повернутись");
        }
    }

    @Nested
    class Parsing {
        @Test
        void fromId_returns_correct_enum() {
            var result = CallbackId.fromId(110);

            assertThat(result)
                    .isPresent()
                    .contains(CallbackId.PROFILE);
        }

        @Test
        void fromId_returns_empty_for_unknown() {
            var result = CallbackId.fromId(999);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class Serialization {
        @Test
        void toCallbackData_creates_string_with_all_fields() {
            var callbackId = CallbackId.PROFILE;
            var result = callbackId.toCallbackData(null, null);

            assertThat(result).isEqualTo("110,,");
        }

        @Test
        void toCallbackData_includes_entityId() {
            var callbackId = CallbackId.PET_DETAIL;
            var result = callbackId.toCallbackData(42L, null);

            assertThat(result).isEqualTo("123,42,");
        }

        @Test
        void toCallbackData_includes_offset() {
            var callbackId = CallbackId.FEED_VIEW;
            var result = callbackId.toCallbackData(null, 10);

            assertThat(result).isEqualTo("135,,10");
        }

        @Test
        void toCallbackData_includes_all_fields() {
            var callbackId = CallbackId.PET_DETAIL;
            var result = callbackId.toCallbackData(42L, 5);

            assertThat(result).isEqualTo("123,42,5");
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
