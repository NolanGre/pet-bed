package op.edu.ua.petbed.telegram.callback.handler.feed;

import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.form.scheme.FormType;
import op.edu.ua.petbed.telegram.service.FormService;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;

@NullMarked
@Component
public class FeedGeolocationCallbackHandler implements CallbackHandler {

    private final FormService formService;

    public FeedGeolocationCallbackHandler(FormService formService) {
        this.formService = formService;
    }

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.FEED_GEOLOCATION;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        return formService.startCreateForm(
                FormType.SET_GEOLOCATION,
                CallbackId.FEED,
                context.auth().userInternalId(),
                context.chatId()
        );
    }
}