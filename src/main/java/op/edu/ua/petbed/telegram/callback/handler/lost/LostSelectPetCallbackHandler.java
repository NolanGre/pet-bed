package op.edu.ua.petbed.telegram.callback.handler.lost;

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
 * Handler for LOST_SELECT_PET callback - initiates CREATE_LOST_REQUEST form for selected pet.
 * The petId is passed via callback entityId and stored in form data.
 */
@NullMarked
@Component
@RequiredArgsConstructor
public class LostSelectPetCallbackHandler implements CallbackHandler {

    private final FormService formService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.LOST_SELECT_PET;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        Long petId = context.callbackData().entityId();
        if (petId == null) {
            throw new PetBedException("Pet ID is required for LOST_SELECT_PET", PetBedException.ErrorCode.INVALID_CALLBACK);
        }

        // Start CREATE_LOST_REQUEST form with the selected petId
        // Using startUpdateForm because we're associating with an existing entity (pet)
        return formService.startUpdateForm(
                FormType.CREATE_LOST_REQUEST,
                CallbackId.LOST_START,  // Return to LOST_START if cancelled
                context.auth().userInternalId(),
                context.chatId(),
                petId
        );
    }
}
