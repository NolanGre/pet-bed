package op.edu.ua.petbed.telegram.callback.handler.lost;

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
 * Handler for LOST_ADD_PET callback - initiates ADD_PET form.
 * After adding a pet, user returns to LOST_START to select it for search.
 */
@NullMarked
@Component
@RequiredArgsConstructor
public class LostAddPetCallbackHandler implements CallbackHandler {

    private final FormService formService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.LOST_ADD_PET;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        // Start ADD_PET form
        // Return to LOST_START after completion so user can select the new pet
        return formService.startCreateForm(
                FormType.ADD_PET,
                CallbackId.LOST_START,  // Return to LOST_START after adding pet
                context.auth().userInternalId(),
                context.chatId()
        );
    }
}
