package op.edu.ua.petbed.telegram.callback.handler.pet;

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
public class AddPetCallbackHandler implements CallbackHandler {

    private final FormService formService;

    public AddPetCallbackHandler(FormService formService) {
        this.formService = formService;
    }

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.ADD_PET;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        return formService.startForm(FormType.ADD_PET, CallbackId.MY_PETS, context.userId(), context.chatId());
    }
}
