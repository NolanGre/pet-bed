package op.edu.ua.petbed.telegram.callback;

import org.jspecify.annotations.NullMarked;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;

/**
 * Handler for inline keyboard button callbacks.
 * <p>
 * When user clicks an inline keyboard button, Telegram sends a CallbackQuery.
 * Router dispatches to appropriate CallbackHandler based on {@link CallbackAction}.
 *
 * @see CallbackAction
 * @see CallbackQueryContext
 */
@NullMarked
public interface CallbackHandler {

    CallbackAction getCallbackAction();

    /**
     * Handles the callback query.
     *
     * @param context containing callback data, user info, message info
     * @return BotApiMethod response (e.g., EditMessageText, AnswerCallbackQuery)
     */
    BotApiMethod<?> handle(CallbackQueryContext context);
}