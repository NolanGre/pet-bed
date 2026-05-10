package op.edu.ua.petbed.telegram.callback.handler.fostering;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.form.scheme.FormType;
import op.edu.ua.petbed.telegram.service.FormService;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;

/**
 * Handler for FOSTERING_ADD_PET callback - starts ADD_PET form from fostering flow.
 */
@NullMarked
@Component
@RequiredArgsConstructor
@Slf4j
public class FosteringAddPetCallbackHandler implements CallbackHandler {

    private final FormService formService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.FOSTERING_ADD_PET;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        return formService.startCreateForm(
                FormType.ADD_PET,
                CallbackId.FOSTERING,
                context.auth().userInternalId(),
                context.chatId()
        );
    }
}