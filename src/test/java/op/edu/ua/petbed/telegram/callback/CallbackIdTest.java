package op.edu.ua.petbed.telegram.callback;

import op.edu.ua.petbed.common.exceptions.PetBedException;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CallbackIdTest {

    @Nested
    class Factory_methods {
        @Test
        void id_returns_correct_value() {
            var result = CallbackId.PROFILE;

            assertThat(result.id()).isEqualTo(2);
        }

        @Test
        void label_returns_correct_value() {
            var result = CallbackId.PROFILE;

            assertThat(result.label()).isEqualTo("👤 Профіль");
        }
    }

    @Nested
    class Hierarchy {
        @Test
        void allIdsShouldBeUnique() {
            Map<Integer, List<CallbackId>> duplicates = Arrays.stream(CallbackId.values())
                    .collect(Collectors.groupingBy(CallbackId::id))
                    .entrySet().stream()
                    .filter(e -> e.getValue().size() > 1)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            assertTrue(duplicates.isEmpty(),
                    "Found duplicate CallbackId values:\n" +
                            duplicates.entrySet().stream()
                                    .map(e -> "  id=%d → %s".formatted(
                                            e.getKey(),
                                            e.getValue().stream()
                                                    .map(Enum::name)
                                                    .collect(Collectors.joining(", "))
                                    ))
                                    .collect(Collectors.joining("\n"))
            );
        }

        @Test
        void children_returns_direct_children_only() {
            var menu = CallbackId.MENU;
            var children = menu.children();

            assertThat(children)
                    .hasSize(6)
                    .contains(CallbackId.PROFILE, CallbackId.MY_PETS, CallbackId.FEED, CallbackId.LOST, CallbackId.FOSTERING, CallbackId.ADOPTION);
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
            var result = CallbackId.fromId(2);

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
                    .contains("MENU[\"🏠 Меню\"]")
                    .contains("MENU --> PROFILE")
                    .contains("PROFILE[\"👤 Профіль\"]");
        }
    }

    @Nested
    class IsPaginated {

        @Test
        void with_paginated_parent_true() {
            // MY_PETS has children like PET_DETAIL with empty label
            var myPets = CallbackId.MY_PETS;

            var result = myPets.isPaginated();

            assertThat(result).isTrue();
        }

        @Test
        void without_children_false() {
            // PROFILE has no children with empty label
            var profile = CallbackId.PROFILE;

            var result = profile.isPaginated();

            assertThat(result).isFalse();
        }

        @Test
        void feed_view_not_paginated() {
            // FEED_VIEW's children don't have empty labels
            var feedView = CallbackId.FEED_VIEW;

            var result = feedView.isPaginated();

            assertThat(result).isFalse();
        }

        @Test
        void lostActiveDetail_not_paginated() {
            // LOST_ACTIVE_DETAIL's direct children don't have empty labels
            var lostActiveDetail = CallbackId.LOST_ACTIVE_DETAIL;

            var result = lostActiveDetail.isPaginated();

            assertThat(result).isFalse();
        }
    }
}
