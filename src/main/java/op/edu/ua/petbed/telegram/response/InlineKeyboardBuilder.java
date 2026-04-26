package op.edu.ua.petbed.telegram.response;

import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.telegram.callback.CallbackData;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

@Slf4j
@NullMarked
public class InlineKeyboardBuilder {

    private enum AddedMethod {
        NAV_BUTTONS, BACK_BUTTON, PAGINATION
    }

    private final List<InlineKeyboardButton> pagination = new ArrayList<>();
    private final List<InlineKeyboardButton> items = new ArrayList<>();
    private final List<InlineKeyboardButton> navButtons = new ArrayList<>();
    private @Nullable InlineKeyboardButton backButton = null;

    private final EnumSet<AddedMethod> added = EnumSet.noneOf(AddedMethod.class);

    private KeyboardLayout layout = KeyboardLayout.DEFAULT;

    private InlineKeyboardBuilder() {
    }

    public static InlineKeyboardBuilder builder() {
        return new InlineKeyboardBuilder();
    }

    public InlineKeyboardBuilder layout(KeyboardLayout layout) {
        this.layout = layout;
        return this;
    }

    public InlineKeyboardMarkup build() {
        List<List<InlineKeyboardButton>> rows = layout.arrange(pagination, items, navButtons, backButton);
        List<InlineKeyboardRow> keyboardRows = rows.stream()
                .map(InlineKeyboardRow::new)
                .toList();
        return InlineKeyboardMarkup.builder()
                .keyboard(keyboardRows)
                .build();
    }

    public InlineKeyboardBuilder navButtonsFor(CallbackId currentCallbackId) {
        checkNotAdded(AddedMethod.NAV_BUTTONS, "navButtonsFor");
        return navButtonsForInternal(currentCallbackId, null, null);
    }

    public InlineKeyboardBuilder navButtonsFor(CallbackId currentCallbackId, @Nullable Long entityId) {
        checkNotAdded(AddedMethod.NAV_BUTTONS, "navButtonsFor");
        return navButtonsForInternal(currentCallbackId, entityId, null);
    }

    public InlineKeyboardBuilder navButtonsFor(CallbackId currentCallbackId, Predicate<CallbackId> filter) {
        checkNotAdded(AddedMethod.NAV_BUTTONS, "navButtonsFor");
        return navButtonsForInternal(currentCallbackId, null, filter);
    }

    public InlineKeyboardBuilder navButtonsFor(CallbackId currentCallbackId, @Nullable Long entityId, Predicate<CallbackId> filter) {
        checkNotAdded(AddedMethod.NAV_BUTTONS, "navButtonsFor");
        return navButtonsForInternal(currentCallbackId, entityId, filter);
    }

    private InlineKeyboardBuilder navButtonsForInternal(CallbackId currentCallbackId, @Nullable Long entityId, @Nullable Predicate<CallbackId> filter) {
        List<CallbackId> children = currentCallbackId.children();
        for (CallbackId child : children) {
            if (filter != null && !filter.test(child)) {
                continue;
            }
            String label = child.label();
            if (label != null && !label.isBlank()) {
                Integer offset = child.isPaginated() ? 0 : null;
                String callbackData = CallbackData.of(child, entityId, offset).toString();
                navButtons.add(createButton(label, callbackData));
            }
        }
        return this;
    }

    public InlineKeyboardBuilder backButtonFor(CallbackId currentCallbackId) {
        checkNotAdded(AddedMethod.BACK_BUTTON, "backButtonFor");
        CallbackId parent = currentCallbackId.parent();
        if (parent != null) {
            Integer offset = parent.isPaginated() ? 0 : null;
            String callbackData = CallbackData.of(parent, null, offset).toString();
            backButton = createButton(CallbackId.BACK_BUTTON_LABEL, callbackData);
        }
        return this;
    }

    public InlineKeyboardBuilder backButtonFor(CallbackId callbackId, Long entityId) {
        checkNotAdded(AddedMethod.BACK_BUTTON, "backButtonFor");
        CallbackId parent = callbackId.parent();
        if (parent != null) {
            Integer offset = parent.isPaginated() ? 0 : null;
            String callbackData = CallbackData.of(parent, entityId, offset).toString();
            backButton = createButton(CallbackId.BACK_BUTTON_LABEL, callbackData);
        }
        return this;
    }

    public InlineKeyboardBuilder backButtonTo(CallbackId callbackId) {
        checkNotAdded(AddedMethod.BACK_BUTTON, "backButtonFor");
        Integer offset = callbackId.isPaginated() ? 0 : null;
        String callbackData = CallbackData.of(callbackId, null, offset).toString();
        backButton = createButton(CallbackId.BACK_BUTTON_LABEL, callbackData);
        return this;
    }

    public InlineKeyboardBuilder backButtonTo(CallbackId callbackId, Long entityId) {
        checkNotAdded(AddedMethod.BACK_BUTTON, "backButtonFor");
        Integer offset = callbackId.isPaginated() ? 0 : null;
        String callbackData = CallbackData.of(callbackId, entityId, offset).toString();
        backButton = createButton(CallbackId.BACK_BUTTON_LABEL, callbackData);
        return this;
    }

    public InlineKeyboardBuilder paginatedList(Page<CallbackListItem> page, CallbackData currentCallbackData) {
        checkNotAdded(AddedMethod.PAGINATION, "paginatedList");

        if (page.hasContent()) {
            log.debug("Page has content. Processing {} items", page.getContent().size());

            for (CallbackListItem item : page.getContent()) {
                String callbackData = CallbackData.of(item.callbackId(), item.entityId(), null).toString();
                items.add(createButton(item.label(), callbackData));
            }

            List<InlineKeyboardButton> paginationButtons = new ArrayList<>();
            if (!page.isFirst()) {
                String prevCallbackData = currentCallbackData.withPrevPage().toString();
                paginationButtons.add(createButton("◀️", prevCallbackData));
                log.debug("Added PREV button: {}", prevCallbackData);
            }

            String pageIndicator = "📄 " + (page.getNumber() + 1) + "/" + page.getTotalPages();
            String indicatorCallbackData = CallbackData.of(CallbackId.PAGINATION_PAGE_INDICATOR).toString();
            paginationButtons.add(createButton(pageIndicator, indicatorCallbackData));

            if (!page.isLast()) {
                String nextCallbackData = currentCallbackData.withNextPage().toString();
                paginationButtons.add(createButton("▶️", nextCallbackData));
                log.debug("Added NEXT button: {}", nextCallbackData);
            }

            pagination.addAll(paginationButtons);
        } else {
            log.info("Page is empty for currentCallbackData: {}", currentCallbackData);
        }

        return this;
    }

    private void checkNotAdded(AddedMethod method, String name) {
        if (added.contains(method)) {
            throw new PetBedException("%s can only be called once".formatted(name), PetBedException.ErrorCode.INTERNAL_ERROR);
        }
        added.add(method);
    }

    private static InlineKeyboardButton createButton(String text, String callbackData) {
        InlineKeyboardButton button = InlineKeyboardButton.builder()
                .text(text)
                .build();
        button.setCallbackData(callbackData);
        return button;
    }
}