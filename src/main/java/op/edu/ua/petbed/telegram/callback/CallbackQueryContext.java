package op.edu.ua.petbed.telegram.callback;

import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.telegram.auth.UserAuthContext;
import org.jspecify.annotations.NullMarked;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * Context record for callback query (inline keyboard button click).
 * <p>
 * Contains all information needed to handle button callback:
 * <ul>
 *     <li>{@link #callbackData()} - parsed action and payload from button</li>
 *     <li>{@link #chatId()} - chat where button was clicked</li>
 *     <li>{@link #messageId()} - message containing the button</li>
 * </ul>
 *
 */
@NullMarked
public record CallbackQueryContext(
        CallbackData callbackData,
        Update update,
        CallbackQuery callbackQuery,
        Long chatId,
        UserAuthContext auth,
        Integer messageId
) {
    public static CallbackQueryContext from(Update update, UserAuthContext userAuthContext) {
        var cb = update.getCallbackQuery();
        var data = CallbackData.from(cb.getData());
        var msg = cb.getMessage();
        if (msg == null) {
            throw new PetBedException("Message is null in CallbackQuery", PetBedException.ErrorCode.INTERNAL_ERROR);
        }
        return new CallbackQueryContext(data, update, cb, msg.getChatId(), userAuthContext, msg.getMessageId());
    }
}