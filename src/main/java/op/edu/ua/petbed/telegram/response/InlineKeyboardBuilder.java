package op.edu.ua.petbed.telegram.response;

import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.telegram.callback.CallbackData;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import org.jspecify.annotations.NullMarked;
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

    private static final String PREV_PAGE = "◀️";
    private static final String NEXT_PAGE = "▶️";

    private final List<List<InlineKeyboardButton>> rows = new ArrayList<>();
    private final EnumSet<AddedMethod> added = EnumSet.noneOf(AddedMethod.class);

    private InlineKeyboardBuilder() {
    }

    public static InlineKeyboardBuilder builder() {
        return new InlineKeyboardBuilder();
    }

    public InlineKeyboardMarkup build() {
        List<InlineKeyboardRow> keyboardRows = rows.stream()
                .map(InlineKeyboardRow::new)
                .toList();
        return InlineKeyboardMarkup.builder()
                .keyboard(keyboardRows)
                .build();
    }

    public InlineKeyboardBuilder navButtonsFor(CallbackId currentCallbackId) {
        checkNotAdded(AddedMethod.NAV_BUTTONS, "navButtonsFor");
        addNavigationButtons(currentCallbackId);
        return this;
    }

    public InlineKeyboardBuilder backButtonFor(CallbackId currentCallbackId) {
        checkNotAdded(AddedMethod.BACK_BUTTON, "backButtonFor");
        CallbackId parent = currentCallbackId.parent();
        if (parent != null) {
            addBackButton(parent);
        }
        return this;
    }

    public InlineKeyboardBuilder backButtonTo(CallbackId callbackId) {
        addBackButton(callbackId);
        return this;
    }

    public InlineKeyboardBuilder paginatedList(Page<CallbackListItem> page, CallbackData currentCallbackData) {
        checkNotAdded(AddedMethod.PAGINATION, "paginatedList");
        addItemButtons(page);
        addPaginationRow(page, currentCallbackData);
        return this;
    }

    private void checkNotAdded(AddedMethod method, String name) {
        if (added.contains(method)) {
            throw new PetBedException("%s can only be called once".formatted(name), PetBedException.ErrorCode.INTERNAL_ERROR);
        }
        added.add(method);
    }

    private void addNavigationButtons(CallbackId currentCallbackId) {
        List<CallbackId> children = currentCallbackId.children();
        List<InlineKeyboardButton> buttons = new ArrayList<>();
        for (CallbackId child : children) {
            String label = child.label();
            if (label != null && !label.isBlank()) {
                String callbackData = CallbackData.of(child, null, null).toString();
                buttons.add(createButton(label, callbackData));
            }
        }
        if (!buttons.isEmpty()) {
            rows.add(buttons);
        }
    }

    private void addBackButton(CallbackId target) {
        String callbackData = CallbackData.of(target, null, null).toString();
        rows.add(List.of(createButton(CallbackId.BACK_BUTTON_LABEL, callbackData)));
    }

    private void addItemButtons(Page<CallbackListItem> page) {
        if (!page.hasContent()) {
            return;
        }
        List<InlineKeyboardButton> itemButtons = new ArrayList<>();
        for (CallbackListItem item : page.getContent()) {
            String callbackData = CallbackData.of(item.callbackId(), item.entityId(), null).toString();
            itemButtons.add(createButton(item.label(), callbackData));
        }
        if (!itemButtons.isEmpty()) {
            rows.add(itemButtons);
        }
    }

    private void addPaginationRow(Page<CallbackListItem> page, CallbackData currentCallbackData) {
        if (!page.hasContent()) {
            return;
        }
        List<InlineKeyboardButton> paginationButtons = new ArrayList<>();

        if (!page.isFirst()) {
            String prevCallbackData = currentCallbackData.withPrevPage().toString();
            paginationButtons.add(createButton(PREV_PAGE, prevCallbackData));
        }

        String pageIndicator = (page.getNumber() + 1) + "/" + page.getTotalPages();
        paginationButtons.add(createButton(pageIndicator, ""));

        if (!page.isLast()) {
            String nextCallbackData = currentCallbackData.withNextPage().toString();
            paginationButtons.add(createButton(NEXT_PAGE, nextCallbackData));
        }

        rows.add(paginationButtons);
    }

    private static InlineKeyboardButton createButton(String text, String callbackData) {
        InlineKeyboardButton button = InlineKeyboardButton.builder()
                .text(text)
                .build();
        button.setCallbackData(callbackData);
        return button;
    }
}