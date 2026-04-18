package op.edu.ua.petbed.telegram.response;

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
        return navButtonsForInternal(currentCallbackId, null);
    }

    public InlineKeyboardBuilder navButtonsFor(CallbackId currentCallbackId, @Nullable Long entityId) {
        checkNotAdded(AddedMethod.NAV_BUTTONS, "navButtonsFor");
        return navButtonsForInternal(currentCallbackId, entityId);
    }

    private InlineKeyboardBuilder navButtonsForInternal(CallbackId currentCallbackId, @Nullable Long entityId) {
        List<CallbackId> children = currentCallbackId.children();
        for (CallbackId child : children) {
            String label = child.label();
            if (label != null && !label.isBlank()) {
                String callbackData = CallbackData.of(child, entityId, null).toString();
                navButtons.add(createButton(label, callbackData));
            }
        }
        return this;
    }

    public InlineKeyboardBuilder backButtonFor(CallbackId currentCallbackId) {
        checkNotAdded(AddedMethod.BACK_BUTTON, "backButtonFor");
        CallbackId parent = currentCallbackId.parent();
        if (parent != null) {
            String callbackData = CallbackData.of(parent, null, null).toString();
            backButton = createButton(CallbackId.BACK_BUTTON_LABEL, callbackData);
        }
        return this;
    }

    public InlineKeyboardBuilder backButtonTo(CallbackId callbackId) {
        String callbackData = CallbackData.of(callbackId, null, null).toString();
        backButton = createButton(CallbackId.BACK_BUTTON_LABEL, callbackData);
        return this;
    }

    public InlineKeyboardBuilder paginatedList(Page<CallbackListItem> page, CallbackData currentCallbackData) {
        checkNotAdded(AddedMethod.PAGINATION, "paginatedList");

        if (page.hasContent()) {
            for (CallbackListItem item : page.getContent()) {
                String callbackData = CallbackData.of(item.callbackId(), item.entityId(), null).toString();
                items.add(createButton(item.label(), callbackData));
            }

            List<InlineKeyboardButton> paginationButtons = new ArrayList<>();
            if (!page.isFirst()) {
                String prevCallbackData = currentCallbackData.withPrevPage().toString();
                paginationButtons.add(createButton("◀️", prevCallbackData));
            }

            String pageIndicator = (page.getNumber() + 1) + "/" + page.getTotalPages();
            paginationButtons.add(createButton(pageIndicator, ""));

            if (!page.isLast()) {
                String nextCallbackData = currentCallbackData.withNextPage().toString();
                paginationButtons.add(createButton("▶️", nextCallbackData));
            }

            pagination.addAll(paginationButtons);
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