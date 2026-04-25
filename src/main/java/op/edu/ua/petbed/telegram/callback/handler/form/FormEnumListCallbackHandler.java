package op.edu.ua.petbed.telegram.callback.handler.form;

import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.service.FormService;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;

@NullMarked
@Component
public class FormEnumListCallbackHandler implements CallbackHandler {

    private final FormService formService;

    public FormEnumListCallbackHandler(FormService formService) {
        this.formService = formService;
    }

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.FORM_ENUM_LIST;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        Integer offset = context.callbackData().offset();
        int page = offset != null ? offset : 0;

        return formService.getEnumKeyboardPage(context.auth().userInternalId(), page, context.messageId());
    }
}