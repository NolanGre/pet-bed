package op.edu.ua.petbed.telegram.callback.handler.fostering;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import op.edu.ua.petbed.telegram.service.FormService;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;

/**
 * Handler for FOSTERING_FINAL_CONFIRM callback - triggers form submission by calling confirmForm.
 */
@NullMarked
@Component
@RequiredArgsConstructor
@Slf4j
public class FosteringFinalConfirmHandler implements CallbackHandler {

    private final FormService formService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.FOSTERING_FINAL_CONFIRM;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        Long userId = context.auth().userInternalId();

        // Call confirmForm which will trigger the form submission handler
        return formService.confirmForm(userId, context.chatId());
    }
}