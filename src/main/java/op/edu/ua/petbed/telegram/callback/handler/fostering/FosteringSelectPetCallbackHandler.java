package op.edu.ua.petbed.telegram.callback.handler.fostering;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * Handler for FOSTERING_SELECT_PET callback - initiates CREATE_FOSTERING_POST form for selected pet.
 * The petId is passed via callback entityId and stored in form data.
 */
@NullMarked
@Component
@RequiredArgsConstructor
@Slf4j
public class FosteringSelectPetCallbackHandler implements CallbackHandler {

    private final FormService formService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.FOSTERING_SELECT_PET;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        Long petId = context.callbackData().entityId();
        if (petId == null) {
            throw new PetBedException("Pet ID is required for FOSTERING_SELECT_PET", PetBedException.ErrorCode.INVALID_CALLBACK);
        }

        return formService.startUpdateForm(
                FormType.CREATE_FOSTERING_POST,
                CallbackId.FOSTERING_GIVE,
                context.auth().userInternalId(),
                context.chatId(),
                petId
        );
    }
}