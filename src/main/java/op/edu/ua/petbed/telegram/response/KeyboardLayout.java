package op.edu.ua.petbed.telegram.response;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

@NullMarked
public record KeyboardLayout(
        int pageSize,
        int maxNavButtonLength
) {
    public static final KeyboardLayout DEFAULT = new KeyboardLayout(5, 35);

    public List<List<InlineKeyboardButton>> arrange(
            List<InlineKeyboardButton> pagination,
            List<InlineKeyboardButton> items,
            List<InlineKeyboardButton> nav,
            @Nullable InlineKeyboardButton back
    ) {
        List<List<InlineKeyboardButton>> result = new ArrayList<>();

        if (!pagination.isEmpty()) {
            result.add(pagination);
        }

        for (InlineKeyboardButton item : items) {
            result.add(List.of(item));
        }

        result.addAll(groupByTwo(nav, maxNavButtonLength));

        if (back != null) {
            result.add(List.of(back));
        }

        return result;
    }

    private List<List<InlineKeyboardButton>> groupByTwo(List<InlineKeyboardButton> buttons, int maxLength) {
        List<List<InlineKeyboardButton>> groups = new ArrayList<>();
        List<InlineKeyboardButton> currentGroup = new ArrayList<>();

        for (InlineKeyboardButton button : buttons) {
            boolean longLabel = button.getText().length() > maxLength;

            if (currentGroup.size() == 2 || (currentGroup.size() == 1 && longLabel)) {
                groups.add(currentGroup);
                currentGroup = new ArrayList<>();
            }

            currentGroup.add(button);
        }

        if (!currentGroup.isEmpty()) {
            groups.add(currentGroup);
        }

        return groups;
    }
}