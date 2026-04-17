package op.edu.ua.petbed.telegram.response;

import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.telegram.callback.CallbackData;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class InlineKeyboardBuilderTest {

    @Test
    void navButtonsFor_with_non_blank_children_creates_buttons() {
        var builder = InlineKeyboardBuilder.builder();
        var result = builder.navButtonsFor(CallbackId.PROFILE).build();

        assertThat(result.getKeyboard()).hasSize(1);
        var row = result.getKeyboard().getFirst();
        assertThat(row).hasSize(1);
    }

    @Test
    void navButtonsFor_chain_build_returns_valid_InlineKeyboardMarkup() {
        var markup = InlineKeyboardBuilder.builder()
                .navButtonsFor(CallbackId.PROFILE)
                .build();

        assertThat(markup).isNotNull();
        assertThat(markup.getKeyboard()).isNotNull();
    }

    @Test
    void navButtonsFor_MENU_has_6_children_creates_6_buttons() {
        var markup = InlineKeyboardBuilder.builder()
                .navButtonsFor(CallbackId.MENU)
                .build();

        assertThat(markup.getKeyboard()).hasSize(3);
        var row = markup.getKeyboard().getFirst();
        assertThat(row).hasSize(2);
    }

    @Test
    void navButtonsFor_with_some_blank_labels_creates_only_non_blank() {
        var markup = InlineKeyboardBuilder.builder()
                .navButtonsFor(CallbackId.PET_LIST)
                .build();

        assertThat(markup.getKeyboard()).isEmpty();
    }

    @Test
    void navButtonsFor_PET_DETAIL_creates_buttons() {
        var markup = InlineKeyboardBuilder.builder()
                .navButtonsFor(CallbackId.PET_DETAIL)
                .build();

        assertThat(markup.getKeyboard()).hasSize(1);

        var row = markup.getKeyboard().getFirst();
        assertThat(row).hasSize(2);
    }

    @Test
    void navButtonsFor_empty_children_list_nothing_added() {
        var markup = InlineKeyboardBuilder.builder()
                .build();

        assertThat(markup.getKeyboard()).isEmpty();
    }

    @Test
    void backButtonFor_with_parent_adds_back_button() {
        var markup = InlineKeyboardBuilder.builder()
                .backButtonFor(CallbackId.PROFILE)
                .build();

        assertThat(markup.getKeyboard()).hasSize(1);
        var row = markup.getKeyboard().getFirst();
        assertThat(row).hasSize(1);
        assertThat(row.getFirst().getText()).isEqualTo(CallbackId.BACK_BUTTON_LABEL);
    }

    @Test
    void backButtonFor_chain_build_returns_valid_InlineKeyboardMarkup() {
        var markup = InlineKeyboardBuilder.builder()
                .backButtonFor(CallbackId.PROFILE)
                .build();

        assertThat(markup).isNotNull();
        assertThat(markup.getKeyboard()).isNotNull();
    }

    @Test
    void backButtonFor_PROFILE_parent_LEADS_to_MENU() {
        var markup = InlineKeyboardBuilder.builder()
                .backButtonFor(CallbackId.PROFILE)
                .build();

        var row = markup.getKeyboard().getFirst();
        assertThat(row.getFirst().getCallbackData())
                .startsWith(String.valueOf(CallbackId.PROFILE.parent().id()));
    }

    @Test
    void backButtonFor_MENU_parent_null_nothing_added() {
        var markup = InlineKeyboardBuilder.builder()
                .backButtonFor(CallbackId.MENU)
                .build();

        assertThat(markup.getKeyboard()).isEmpty();
    }

    @Test
    void backButtonFor_after_navButtonsFor() {
        var markup = InlineKeyboardBuilder.builder()
                .navButtonsFor(CallbackId.PROFILE)
                .backButtonFor(CallbackId.PROFILE)
                .build();

        assertThat(markup.getKeyboard()).hasSize(2);
    }


    @Test
    void backButtonTo_any_callbackId_adds_button() {
        var markup = InlineKeyboardBuilder.builder()
                .backButtonTo(CallbackId.MENU)
                .build();

        assertThat(markup.getKeyboard()).hasSize(1);
        var row = markup.getKeyboard().getFirst();
        assertThat(row.getFirst().getText()).isEqualTo(CallbackId.BACK_BUTTON_LABEL);
    }

    @Test
    void backButtonTo_MENU_always_adds_button() {
        var markup = InlineKeyboardBuilder.builder()
                .backButtonTo(CallbackId.MENU)
                .build();

        assertThat(markup.getKeyboard()).hasSize(1);
    }

    @Test
    void backButtonTo_chain_build_returns_valid_InlineKeyboardMarkup() {
        var markup = InlineKeyboardBuilder.builder()
                .backButtonTo(CallbackId.PROFILE)
                .build();

        assertThat(markup).isNotNull();
        assertThat(markup.getKeyboard()).isNotNull();
    }

    @Test
    void backButtonTo_chain_with_other_methods() {
        var markup = InlineKeyboardBuilder.builder()
                .navButtonsFor(CallbackId.MENU)
                .backButtonTo(CallbackId.PROFILE)
                .build();

        assertThat(markup.getKeyboard()).hasSize(4);
    }

    @Test
    void paginatedList_with_content_1_page() {
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
    void paginatedList_with_more_than_1_page() {
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
    void paginatedList_first_page_no_prev_button() {
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
    void paginatedList_last_page_no_next_button() {
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
    void paginatedList_last_page_one_navigation_buttons() {
        Page<CallbackListItem> page = new PageImpl<>(List.of(
                new CallbackListItem(CallbackId.PET_DETAIL, 1L, "Барсик")
        ), PageRequest.of(1, 1),2);
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
    void paginatedList_chain_build_returns_valid_InlineKeyboardMarkup() {
        List<CallbackListItem> items = List.of(
                new CallbackListItem(CallbackId.PET_DETAIL, 1L, "Барсик")
        );
        Page<CallbackListItem> page = new PageImpl<>(items, PageRequest.of(0, 1), 1);
        var currentCallback = CallbackData.of(CallbackId.PET_LIST, null, page.getNumber()); // Root
        var markup = InlineKeyboardBuilder.builder()
                .paginatedList(page, currentCallback)
                .build();
        assertThat(markup).isNotNull();
    }

    @Test
    void paginatedList_page_without_content_nothing_added() {
        List<CallbackListItem> items = List.of();
        Page<CallbackListItem> page = new PageImpl<>(items, PageRequest.of(0, 1), 0);
        var currentCallback = CallbackData.of(CallbackId.PET_LIST, null, page.getNumber()); // Root
        var markup = InlineKeyboardBuilder.builder()
                .paginatedList(page, currentCallback)
                .build();
        assertThat(markup.getKeyboard()).isEmpty();
    }

    @Test
    void paginatedList_page_with_0_items() {
        Page<CallbackListItem> page = Page.empty(PageRequest.of(0, 1));
        var currentCallback = CallbackData.of(CallbackId.PET_LIST, null, page.getNumber()); // Root
        var markup = InlineKeyboardBuilder.builder()
                .paginatedList(page, currentCallback)
                .build();
        assertThat(markup.getKeyboard()).isEmpty();
    }

    @Test
    void combined_navButtonsFor_backButtonFor_build() {
        var markup = InlineKeyboardBuilder.builder()
                .navButtonsFor(CallbackId.PROFILE)
                .backButtonFor(CallbackId.PROFILE)
                .build();

        assertThat(markup.getKeyboard()).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    void combined_navButtonsFor_paginatedList_build() {
        List<CallbackListItem> items = List.of(
                new CallbackListItem(CallbackId.PET_DETAIL, 1L, "Барсик")
        );
        Page<CallbackListItem> page = new PageImpl<>(items, PageRequest.of(0, 1), 1);
        var currentCallback = CallbackData.of(CallbackId.PET_LIST, null, page.getNumber()); // Root
        var markup = InlineKeyboardBuilder.builder()
                .navButtonsFor(CallbackId.MENU)
                .paginatedList(page, currentCallback)
                .build();
        assertThat(markup.getKeyboard()).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void combined_backButtonFor_paginatedList_build() {
        List<CallbackListItem> items = List.of(
                new CallbackListItem(CallbackId.PET_DETAIL, 1L, "Барсик")
        );
        Page<CallbackListItem> page = new PageImpl<>(items, PageRequest.of(0, 1), 1);
        var currentCallback = CallbackData.of(CallbackId.PET_LIST, null, page.getNumber()); // Root
        var markup = InlineKeyboardBuilder.builder()
                .backButtonFor(CallbackId.PROFILE)
                .paginatedList(page, currentCallback)
                .build();
        assertThat(markup.getKeyboard()).hasSize(3);
    }

    @Test
    void combined_navButtonsFor_backButtonFor_paginatedList_build() {
        List<CallbackListItem> items = List.of(
                new CallbackListItem(CallbackId.PET_DETAIL, 1L, "Барсик")
        );
        Page<CallbackListItem> page = new PageImpl<>(items, PageRequest.of(0, 1), 1);
        var currentCallback = CallbackData.of(CallbackId.PET_LIST, null, page.getNumber()); // Root
        var markup = InlineKeyboardBuilder.builder()
                .navButtonsFor(CallbackId.PROFILE)
                .backButtonFor(CallbackId.PROFILE)
                .paginatedList(page, currentCallback)
                .build();
        assertThat(markup.getKeyboard()).hasSize(4);
    }

    @Test
    void combined_multiple_navButtonsFor_throws_exception() {
        assertThatThrownBy(() -> InlineKeyboardBuilder.builder()
                        .navButtonsFor(CallbackId.MENU)
                        .navButtonsFor(CallbackId.PROFILE)
                        .build())
                .isInstanceOf(PetBedException.class);
    }
}