package op.edu.ua.petbed.telegram.callback.handler.adoption;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.adoption.AdoptionResponseService;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.form.scheme.FormType;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import op.edu.ua.petbed.telegram.service.FormService;
import op.edu.ua.petbed.telegram.service.TelegramMessageService;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

/**
 * Handler for ADOPTION_RESPONSE_CREATE callback - starts CREATE_ADOPTION_RESPONSE form.
 */
@NullMarked
@Component
@RequiredArgsConstructor
public class AdoptionResponseCreateHandler implements CallbackHandler {

    private final FormService formService;
    private final AdoptionResponseService adoptionResponseService;
    private final TelegramMessageService telegramMessageService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.ADOPTION_RESPONSE_CREATE;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        Long postId = context.callbackData().entityId();
        if (postId == null) {
            throw new PetBedException("Post ID is required for ADOPTION_RESPONSE_CREATE", PetBedException.ErrorCode.INVALID_CALLBACK);
        }

        Long userId = context.auth().userInternalId();
        if (adoptionResponseService.hasActiveResponse(postId, userId)) {
            return telegramMessageService.editOrReplace(context, alreadyHaveActiveRespMessage(context));
        }

        return formService.startUpdateForm(
                FormType.CREATE_ADOPTION_RESPONSE,
                CallbackId.ADOPTION_GET,
                userId,
                context.chatId(),
                postId
        );
    }

    private SendMessage alreadyHaveActiveRespMessage(CallbackQueryContext context) {
        return ResponseBuilder.sendMessage(context.chatId())
                .text("Ви вже відгукнулися на це оголошення!")
                .keyboard(InlineKeyboardBuilder.builder()
                        .backButtonTo(CallbackId.ADOPTION)
                        .build())
                .build();
    }
}
