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
 * Handler for ADOPTION_RESPONSE_CREATE callback - starts CREATE_ADOPTION_RESPONSE form.
 */
@NullMarked
@Component
@RequiredArgsConstructor
public class AdoptionResponseCreateHandler implements CallbackHandler {

    private final FormService formService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.ADOPTION_RESPONSE_CREATE;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        Long postId = context.callbackData().entityId();
        if (postId == null) {
            throw new PetBedException("Post ID is required for ADOPTION_RESPONSE_CREATE",
                    PetBedException.ErrorCode.INVALID_CALLBACK);
        }

        // Start CREATE_ADOPTION_RESPONSE form with the postId
        return formService.startUpdateForm(
                FormType.CREATE_ADOPTION_RESPONSE,
                CallbackId.ADOPTION_GET,  // Return to feed if cancelled
                context.auth().userInternalId(),
                context.chatId(),
                postId
        );
    }
}
