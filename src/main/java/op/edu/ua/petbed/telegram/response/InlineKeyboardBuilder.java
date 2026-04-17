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
import java.util.List;

@NullMarked
public class InlineKeyboardBuilder {

    private final List<List<InlineKeyboardButton>> rows = new ArrayList<>();
    private boolean navButtonsAdded = false;

    private InlineKeyboardBuilder() {
    }

    public static InlineKeyboardBuilder builder() {
        return new InlineKeyboardBuilder();
    }

    public InlineKeyboardMarkup build() {
        List<InlineKeyboardRow> keyboardRows = new ArrayList<>();
        for (List<InlineKeyboardButton> row : rows) {
            keyboardRows.add(new InlineKeyboardRow(row));
        }
        return InlineKeyboardMarkup.builder()
                .keyboard(keyboardRows)
                .build();
    }

    public InlineKeyboardBuilder navButtonsFor(CallbackId currentCallbackId) {
        if (navButtonsAdded) {
            throw new PetBedException("navButtonsFor can only be called once", PetBedException.ErrorCode.INTERNAL_ERROR);
        }
        navButtonsAdded = true;

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
        return this;
    }

    public InlineKeyboardBuilder backButtonFor(CallbackId currentCallbackId) {
        CallbackId parent = currentCallbackId.parent();
        if (parent != null) {
            String callbackData = CallbackData.of(parent, null, null).toString();
            rows.add(List.of(createButton(CallbackId.BACK_BUTTON_LABEL, callbackData)));
        }
        return this;
    }

    public InlineKeyboardBuilder backButtonTo(CallbackId callbackId) {
        String callbackData = CallbackData.of(callbackId, null, null).toString();
        rows.add(List.of(createButton(CallbackId.BACK_BUTTON_LABEL, callbackData)));
        return this;
    }

    public InlineKeyboardBuilder paginatedList(Page<CallbackListItem> page, CallbackData currentCallbackData) {
        // Add list items as buttons
        if (page.hasContent()) {
            List<InlineKeyboardButton> itemButtons = new ArrayList<>();
            for (CallbackListItem item : page.getContent()) {
                String callbackData = CallbackData.of(item.callbackId(), item.entityId(), null).toString();
                itemButtons.add(createButton(item.label(), callbackData));
            }
            if (!itemButtons.isEmpty()) {
                rows.add(itemButtons);
            }
        }

        // Add pagination row if there are pages
        if (page.hasContent()) {
            List<InlineKeyboardButton> paginationButtons = new ArrayList<>();

            // Previous button (only if not first)
            if (!page.isFirst()) {
                String prevCallbackData = currentCallbackData.withPrevPage().toString();
                paginationButtons.add(createButton("◀️", prevCallbackData));
            }

            // Page indicator
            String pageIndicator = (page.getNumber() + 1) + "/" + page.getTotalPages();
            paginationButtons.add(createButton(pageIndicator, CallbackData.of(CallbackId.PAGINATION_PAGE_INDICATOR).toString()));

            // Next button (only if not last)
            if (!page.isLast()) {
                String nextCallbackData = currentCallbackData.withNextPage().toString();
                paginationButtons.add(createButton("▶️", nextCallbackData));
            }

            rows.add(paginationButtons);
        }

        return this;
    }

    private static InlineKeyboardButton createButton(String text, String callbackData) {
        InlineKeyboardButton button = InlineKeyboardButton.builder()
                .text(text)
                .build();
        button.setCallbackData(callbackData);
        return button;
    }


}