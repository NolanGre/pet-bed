package op.edu.ua.petbed.telegram.callback.handler.adoption;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.form.scheme.FormType;
import op.edu.ua.petbed.telegram.service.FormService;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;

/**
 * Handler for ADOPTION_SELECT_PET callback - initiates CREATE_ADOPTION_POST form for selected pet.
 */
@NullMarked
@Component
@RequiredArgsConstructor
public class AdoptionSelectPetCallbackHandler implements CallbackHandler {

    private final FormService formService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.ADOPTION_SELECT_PET;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        Long petId = context.callbackData().entityId();
        if (petId == null) {
            throw new PetBedException("Pet ID is required for ADOPTION_SELECT_PET",
                    PetBedException.ErrorCode.INVALID_CALLBACK);
        }

        return formService.startUpdateForm(
                FormType.CREATE_ADOPTION_POST,
                CallbackId.ADOPTION_GIVE,
                context.auth().userInternalId(),
                context.chatId(),
                petId
        );
    }
}
