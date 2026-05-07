package op.edu.ua.petbed.telegram.callback.handler.adoption;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.form.scheme.FormType;
import op.edu.ua.petbed.telegram.service.FormService;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;

/**
 * Handler for ADOPTION_ADD_PET callback - starts ADD_PET form from adoption flow.
 */
@NullMarked
@Component
@RequiredArgsConstructor
public class AdoptionAddPetCallbackHandler implements CallbackHandler {

    private final FormService formService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.ADOPTION_ADD_PET;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        // Start ADD_PET form, return to ADOPTION_GIVE after completion
        return formService.startCreateForm(
                FormType.ADD_PET,
                CallbackId.ADOPTION_GIVE,
                context.auth().userInternalId(),
                context.chatId()
        );
    }
}
