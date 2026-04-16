package op.edu.ua.petbed.telegram.callback;

import org.jspecify.annotations.NullMarked;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;

@NullMarked
public interface CallbackHandler {

    CallbackId getCallbackId();

    BotApiMethod<?> handle(CallbackQueryContext context);
}