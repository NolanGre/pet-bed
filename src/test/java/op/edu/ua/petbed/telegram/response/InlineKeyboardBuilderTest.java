package op.edu.ua.petbed.telegram.response;

import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.telegram.callback.CallbackData;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InlineKeyboardBuilderTest {

    @Nested
    @DisplayName("navButtonsFor(CallbackId, Long)")
    class NavButtonsForWithEntityId {

        @Test
        void returns_builder_for_chaining() {
            var builder = InlineKeyboardBuilder.builder();
            var result = builder.navButtonsFor(CallbackId.PROFILE, 123L);

            assertThat(result).isSameAs(builder);
        }

        @Test
        void creates_buttons_with_entity_in_callbackData() {
            Long entityId = 456L;

            var markup = InlineKeyboardBuilder.builder()
                    .navButtonsFor(CallbackId.PROFILE, entityId)
                    .build();

            assertThat(markup.getKeyboard()).isNotEmpty();
            var row = markup.getKeyboard().getFirst();
            var button = row.getFirst();

            String callbackData = button.getCallbackData();
            assertThat(callbackData).isNotBlank();
            assertThat(callbackData).contains("456");
        }

        @Test
        void with_null_entityId_creates_buttons_without_entity() {
            var markup = InlineKeyboardBuilder.builder()
                    .navButtonsFor(CallbackId.PROFILE, null)
                    .build();

            assertThat(markup.getKeyboard()).isNotEmpty();
            var row = markup.getKeyboard().getFirst();
            var button = row.getFirst();

            String callbackData = button.getCallbackData();
            // When entityId is null, format is "callbackId,," (null entity becomes empty)
            assertThat(callbackData).contains(",,");
        }

        @Test
        void followed_by_backButton() {
            Long entityId = 789L;

            var markup = InlineKeyboardBuilder.builder()
                    .navButtonsFor(CallbackId.PROFILE, entityId)
                    .backButtonFor(CallbackId.PROFILE)
                    .build();

            assertThat(markup.getKeyboard()).hasSize(2);
        }

        @Test
        void chain_build_returns_valid_InlineKeyboardMarkup() {
            var markup = InlineKeyboardBuilder.builder()
                    .navButtonsFor(CallbackId.PROFILE, 100L)
                    .build();

            assertThat(markup).isNotNull();
            assertThat(markup.getKeyboard()).isNotNull();
        }

        @Test
        void PET_LIST_with_entityId_no_buttons_for_blank_labels() {
            var markup = InlineKeyboardBuilder.builder()
                    .navButtonsFor(CallbackId.PET_LIST, 1L)
                    .build();

            assertThat(markup.getKeyboard()).isEmpty();
        }

        @Test
        void MENU_with_entityId_creates_correct_callback_data() {
            Long entityId = 42L;

            var markup = InlineKeyboardBuilder.builder()
                    .navButtonsFor(CallbackId.MENU, entityId)
                    .build();

            assertThat(markup.getKeyboard()).hasSize(3);
        }

        @Test
        void allows_method_chaining() {
            var markup = InlineKeyboardBuilder.builder()
                    .navButtonsFor(CallbackId.PROFILE, 1L)
                    .backButtonFor(CallbackId.PROFILE)
                    .build();

            assertThat(markup).isNotNull();
            assertThat(markup.getKeyboard()).hasSizeGreaterThanOrEqualTo(1);
        }

        @Test
        void callbackData_format_correct() {
            Long entityId = 999L;

            var markup = InlineKeyboardBuilder.builder()
                    .navButtonsFor(CallbackId.PROFILE, entityId)
                    .build();

            var button = markup.getKeyboard().getFirst().getFirst();
            CallbackData parsed = CallbackData.from(button.getCallbackData());

            assertThat(parsed.callbackId()).isEqualTo(CallbackId.PROFILE_CHANGE_TYPE.id());
            assertThat(parsed.entityId()).isEqualTo(999L);
        }
    }

    @Nested
    @DisplayName("navButtonsFor(CallbackId)")
    class NavButtonsForWithoutEntityId {

        @Test
        void with_non_blank_children_creates_buttons() {
            var builder = InlineKeyboardBuilder.builder();
            var result = builder.navButtonsFor(CallbackId.PROFILE).build();

            assertThat(result.getKeyboard()).hasSize(1);
            var row = result.getKeyboard().getFirst();
            assertThat(row).hasSize(1);
        }

        @Test
        void chain_build_returns_valid_InlineKeyboardMarkup() {
            var markup = InlineKeyboardBuilder.builder()
                    .navButtonsFor(CallbackId.PROFILE)
                    .build();

            assertThat(markup).isNotNull();
            assertThat(markup.getKeyboard()).isNotNull();
        }

        @Test
        void MENU_has_6_children_creates_6_buttons() {
            var markup = InlineKeyboardBuilder.builder()
                    .navButtonsFor(CallbackId.MENU)
                    .build();

            assertThat(markup.getKeyboard()).hasSize(3);
            var row = markup.getKeyboard().getFirst();
            assertThat(row).hasSize(2);
        }

        @Test
        void with_some_blank_labels_creates_only_non_blank() {
            var markup = InlineKeyboardBuilder.builder()
                    .navButtonsFor(CallbackId.PET_LIST)
                    .build();

            assertThat(markup.getKeyboard()).isEmpty();
        }

        @Test
        void PET_DETAIL_creates_buttons() {
            var markup = InlineKeyboardBuilder.builder()
                    .navButtonsFor(CallbackId.PET_DETAIL)
                    .build();

            assertThat(markup.getKeyboard()).hasSize(1);

            var row = markup.getKeyboard().getFirst();
            assertThat(row).hasSize(2);
        }

        @Test
        void empty_children_list_nothing_added() {
            var markup = InlineKeyboardBuilder.builder()
                    .build();

            assertThat(markup.getKeyboard()).isEmpty();
        }
    }

    @Nested
    @DisplayName("backButtonFor(CallbackId)")
    class BackButtonFor {

        @Test
        void with_parent_adds_back_button() {
            var markup = InlineKeyboardBuilder.builder()
                    .backButtonFor(CallbackId.PROFILE)
                    .build();

            assertThat(markup.getKeyboard()).hasSize(1);
            var row = markup.getKeyboard().getFirst();
            assertThat(row).hasSize(1);
            assertThat(row.getFirst().getText()).isEqualTo(CallbackId.BACK_BUTTON_LABEL);
        }

        @Test
        void chain_build_returns_valid_InlineKeyboardMarkup() {
            var markup = InlineKeyboardBuilder.builder()
                    .backButtonFor(CallbackId.PROFILE)
                    .build();

            assertThat(markup).isNotNull();
            assertThat(markup.getKeyboard()).isNotNull();
        }

        @Test
        void PROFILE_parent_LEADS_to_MENU() {
            var markup = InlineKeyboardBuilder.builder()
                    .backButtonFor(CallbackId.PROFILE)
                    .build();

            var row = markup.getKeyboard().getFirst();
            assertThat(row.getFirst().getCallbackData())
                    .startsWith(String.valueOf(CallbackId.PROFILE.parent().id()));
        }

        @Test
        void MENU_parent_null_nothing_added() {
            var markup = InlineKeyboardBuilder.builder()
                    .backButtonFor(CallbackId.MENU)
                    .build();

            assertThat(markup.getKeyboard()).isEmpty();
        }

        @Test
        void after_navButtonsFor() {
            var markup = InlineKeyboardBuilder.builder()
                    .navButtonsFor(CallbackId.PROFILE)
                    .backButtonFor(CallbackId.PROFILE)
                    .build();

            assertThat(markup.getKeyboard()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("backButtonTo(CallbackId)")
    class BackButtonTo {

        @Test
        void any_callbackId_adds_button() {
            var markup = InlineKeyboardBuilder.builder()
                    .backButtonTo(CallbackId.MENU)
                    .build();

            assertThat(markup.getKeyboard()).hasSize(1);
            var row = markup.getKeyboard().getFirst();
            assertThat(row.getFirst().getText()).isEqualTo(CallbackId.BACK_BUTTON_LABEL);
        }

        @Test
        void MENU_always_adds_button() {
            var markup = InlineKeyboardBuilder.builder()
                    .backButtonTo(CallbackId.MENU)
                    .build();

            assertThat(markup.getKeyboard()).hasSize(1);
        }

        @Test
        void chain_build_returns_valid_InlineKeyboardMarkup() {
            var markup = InlineKeyboardBuilder.builder()
                    .backButtonTo(CallbackId.PROFILE)
                    .build();

            assertThat(markup).isNotNull();
            assertThat(markup.getKeyboard()).isNotNull();
        }

        @Test
        void chain_with_other_methods() {
            var markup = InlineKeyboardBuilder.builder()
                    .navButtonsFor(CallbackId.MENU)
                    .backButtonTo(CallbackId.PROFILE)
                    .build();

            assertThat(markup.getKeyboard()).hasSize(4);
        }
    }

    @Nested
    @DisplayName("paginatedList(Page, CallbackData)")
    class PaginatedList {

        @Test
        void with_content_1_page() {
            Page<CallbackListItem> page = new PageImpl<>(List.of(
                    new CallbackListItem(CallbackId.PET_DETAIL, 1L, "Барсик")
            ), PageRequest.of(0, 1), 1);
            var currentCallback = CallbackData.of(CallbackId.PET_LIST, null, page.getNumber());

            var markup = InlineKeyboardBuilder.builder()
                    .paginatedList(page, currentCallback)
                    .build();

            assertThat(markup.getKeyboard()).hasSize(2);
        }

        @Test
        void with_more_than_1_page() {
            Page<CallbackListItem> page = new PageImpl<>(List.of(
                    new CallbackListItem(CallbackId.PET_DETAIL, 1L, "Барсик"),
                    new CallbackListItem(CallbackId.PET_DETAIL, 2L, "Мурчик")
            ), PageRequest.of(1, 1), 2);
            var currentCallback = CallbackData.of(CallbackId.PET_LIST, null, page.getNumber());

            var markup = InlineKeyboardBuilder.builder()
                    .paginatedList(page, currentCallback)
                    .build();

            assertThat(markup.getKeyboard()).hasSize(3);
            var paginationRow = markup.getKeyboard().getFirst();
            assertThat(paginationRow).hasSize(2);
        }

        @Test
        void first_page_no_prev_button() {
            Page<CallbackListItem> page = new PageImpl<>(List.of(
                    new CallbackListItem(CallbackId.PET_DETAIL, 1L, "Барсик")
            ), PageRequest.of(0, 1), 1);
            var currentCallback = CallbackData.of(CallbackId.PET_LIST, null, page.getNumber());

            var markup = InlineKeyboardBuilder.builder()
                    .paginatedList(page, currentCallback)
                    .build();

            var paginationRow = markup.getKeyboard().getFirst();
            boolean hasPrev = paginationRow.stream()
                    .anyMatch(b -> b.getText().equals("◀️"));
            assertThat(hasPrev).isFalse();
        }

        @Test
        void last_page_no_next_button() {
            Page<CallbackListItem> page = new PageImpl<>(List.of(
                    new CallbackListItem(CallbackId.PET_DETAIL, 2L, "Мурчик")
            ), PageRequest.of(0, 1), 1);
            var currentCallback = CallbackData.of(CallbackId.PET_LIST, null, page.getNumber());

            var markup = InlineKeyboardBuilder.builder()
                    .paginatedList(page, currentCallback)
                    .build();

            var paginationRow = markup.getKeyboard().getFirst();
            boolean hasNext = paginationRow.stream()
                    .anyMatch(b -> b.getText().equals("▶️"));
            assertThat(hasNext).isFalse();
        }

        @Test
        void last_page_one_navigation_buttons() {
            Page<CallbackListItem> page = new PageImpl<>(List.of(
                    new CallbackListItem(CallbackId.PET_DETAIL, 1L, "Барсик")
            ), PageRequest.of(1, 1), 2);
            var currentCallback = CallbackData.of(CallbackId.PET_LIST, null, page.getNumber());

            var markup = InlineKeyboardBuilder.builder()
                    .paginatedList(page, currentCallback)
                    .build();

            var paginationRow = markup.getKeyboard().getFirst();
            boolean hasPrev = paginationRow.stream().anyMatch(b -> b.getText().equals("◀️"));
            boolean hasNext = paginationRow.stream().anyMatch(b -> b.getText().equals("▶️"));
            assertThat(hasPrev).isTrue();
            assertThat(hasNext).isFalse();
        }

        @Test
        void chain_build_returns_valid_InlineKeyboardMarkup() {
            List<CallbackListItem> items = List.of(
                    new CallbackListItem(CallbackId.PET_DETAIL, 1L, "Барсик")
            );
            Page<CallbackListItem> page = new PageImpl<>(items, PageRequest.of(0, 1), 1);
            var currentCallback = CallbackData.of(CallbackId.PET_LIST, null, page.getNumber());
            var markup = InlineKeyboardBuilder.builder()
                    .paginatedList(page, currentCallback)
                    .build();
            assertThat(markup).isNotNull();
        }

        @Test
        void page_without_content_nothing_added() {
            List<CallbackListItem> items = List.of();
            Page<CallbackListItem> page = new PageImpl<>(items, PageRequest.of(0, 1), 0);
            var currentCallback = CallbackData.of(CallbackId.PET_LIST, null, page.getNumber());
            var markup = InlineKeyboardBuilder.builder()
                    .paginatedList(page, currentCallback)
                    .build();
            assertThat(markup.getKeyboard()).isEmpty();
        }

        @Test
        void page_with_0_items() {
            Page<CallbackListItem> page = Page.empty(PageRequest.of(0, 1));
            var currentCallback = CallbackData.of(CallbackId.PET_LIST, null, page.getNumber());
            var markup = InlineKeyboardBuilder.builder()
                    .paginatedList(page, currentCallback)
                    .build();
            assertThat(markup.getKeyboard()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Combined chains")
    class CombinedChains {

        @Test
        void navButtonsFor_backButtonFor_build() {
            var markup = InlineKeyboardBuilder.builder()
                    .navButtonsFor(CallbackId.PROFILE)
                    .backButtonFor(CallbackId.PROFILE)
                    .build();

            assertThat(markup.getKeyboard()).hasSizeGreaterThanOrEqualTo(1);
        }

        @Test
        void navButtonsFor_paginatedList_build() {
            List<CallbackListItem> items = List.of(
                    new CallbackListItem(CallbackId.PET_DETAIL, 1L, "Барсик")
            );
            Page<CallbackListItem> page = new PageImpl<>(items, PageRequest.of(0, 1), 1);
            var currentCallback = CallbackData.of(CallbackId.PET_LIST, null, page.getNumber());
            var markup = InlineKeyboardBuilder.builder()
                    .navButtonsFor(CallbackId.MENU)
                    .paginatedList(page, currentCallback)
                    .build();
            assertThat(markup.getKeyboard()).hasSizeGreaterThanOrEqualTo(2);
        }

        @Test
        void backButtonFor_paginatedList_build() {
            List<CallbackListItem> items = List.of(
                    new CallbackListItem(CallbackId.PET_DETAIL, 1L, "Барсик")
            );
            Page<CallbackListItem> page = new PageImpl<>(items, PageRequest.of(0, 1), 1);
            var currentCallback = CallbackData.of(CallbackId.PET_LIST, null, page.getNumber());
            var markup = InlineKeyboardBuilder.builder()
                    .backButtonFor(CallbackId.PROFILE)
                    .paginatedList(page, currentCallback)
                    .build();
            assertThat(markup.getKeyboard()).hasSize(3);
        }

        @Test
        void navButtonsFor_backButtonFor_paginatedList_build() {
            List<CallbackListItem> items = List.of(
                    new CallbackListItem(CallbackId.PET_DETAIL, 1L, "Барсик")
            );
            Page<CallbackListItem> page = new PageImpl<>(items, PageRequest.of(0, 1), 1);
            var currentCallback = CallbackData.of(CallbackId.PET_LIST, null, page.getNumber());
            var markup = InlineKeyboardBuilder.builder()
                    .navButtonsFor(CallbackId.PROFILE)
                    .backButtonFor(CallbackId.PROFILE)
                    .paginatedList(page, currentCallback)
                    .build();
            assertThat(markup.getKeyboard()).hasSize(4);
        }

        @Test
        void multiple_navButtonsFor_throws_exception() {
            assertThatThrownBy(() -> InlineKeyboardBuilder.builder()
                            .navButtonsFor(CallbackId.MENU)
                            .navButtonsFor(CallbackId.PROFILE)
                            .build())
                    .isInstanceOf(PetBedException.class);
        }
    }
}