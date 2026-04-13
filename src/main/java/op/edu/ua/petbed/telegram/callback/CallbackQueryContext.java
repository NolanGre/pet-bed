package op.edu.ua.petbed.telegram.callback;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * Context record for callback query (inline keyboard button click).
 * <p>
 * Contains all information needed to handle button callback:
 * <ul>
 *     <li>{@link #callbackData()} - parsed action and payload from button</li>
 *     <li>{@link #userId()} - user who clicked</li>
 *     <li>{@link #chatId()} - chat where button was clicked</li>
 *     <li>{@link #messageId()} - message containing the button</li>
 * </ul>
 *
 * <b>Accessing callback data:</b>
 * <pre>
 * context.callbackData().action()     // CallbackAction enum
 * context.callbackData().payload()   // optional string payload
 * context.callbackData().offset()     // optional pagination offset
 * </pre>
 *
 * @see CallbackData
 * @see CallbackAction
 */
@NullMarked
public record CallbackQueryContext(
    CallbackData callbackData,
    Update update,
    CallbackQuery callbackQuery,
    Long chatId,
    Long userId,
    @Nullable String username,
    @Nullable Integer messageId
) {
    /**
     * Creates context from Telegram Update containing CallbackQuery.
     *
     * @param update with CallbackQuery
     * @return parsed CallbackQueryContext
     */
    public static CallbackQueryContext from(Update update) {
        var cb = update.getCallbackQuery();
        var data = CallbackData.from(cb.getData());
        var msg = cb.getMessage();
        return new CallbackQueryContext(
            data,
            update,
            cb,
            msg != null ? msg.getChatId() : null,
            cb.getFrom().getId(),
            cb.getFrom().getUserName(),
            msg != null ? msg.getMessageId() : null
        );
    }
}