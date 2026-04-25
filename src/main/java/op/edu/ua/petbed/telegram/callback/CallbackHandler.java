package op.edu.ua.petbed.telegram.callback;

import org.jspecify.annotations.NullMarked;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;

@NullMarked
public interface CallbackHandler {

    CallbackId getCallbackId();

    PartialBotApiMethod<?> handle(CallbackQueryContext context);
}