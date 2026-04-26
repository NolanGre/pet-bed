package op.edu.ua.petbed.telegram.callback.handler.pet;

import op.edu.ua.petbed.common.exceptions.PetBedException;
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
public class UpdatePetCallbackHandler implements CallbackHandler {

    private final FormService formService;

    public UpdatePetCallbackHandler(FormService formService) {
        this.formService = formService;
    }

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.PET_UPDATE;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        var entityId = context.callbackData().entityId();
        if (entityId == null) {
            throw new PetBedException("Entity ID is required for PET_UPDATE", PetBedException.ErrorCode.INVALID_CALLBACK);
        }

        return formService.startUpdateForm(
                FormType.UPDATE_PET,
                CallbackId.PET_DETAIL,
                context.auth().userInternalId(),
                context.chatId(),
                entityId
        );
    }
}