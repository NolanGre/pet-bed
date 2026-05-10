package op.edu.ua.petbed.telegram.callback.handler.fostering;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.fostering.FosteringResponseService;
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
 * Handler for FOSTERING_RESPONSE_CREATE callback - starts CREATE_FOSTERING_RESPONSE form.
 */
@NullMarked
@Component
@RequiredArgsConstructor
@Slf4j
public class FosteringResponseCreateHandler implements CallbackHandler {

    private final FormService formService;
    private final FosteringResponseService fosteringResponseService;
    private final TelegramMessageService telegramMessageService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.FOSTERING_RESPONSE_CREATE;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        Long postId = context.callbackData().entityId();
        if (postId == null) {
            throw new PetBedException("Post ID is required for FOSTERING_RESPONSE_CREATE", PetBedException.ErrorCode.INVALID_CALLBACK);
        }

        Long userId = context.auth().userInternalId();
        if (fosteringResponseService.hasActiveResponse(postId, userId)) {
            return telegramMessageService.editOrReplace(context, alreadyHaveActiveRespMessage(context));
        }

        return formService.startUpdateForm(
                FormType.CREATE_FOSTERING_RESPONSE,
                CallbackId.FOSTERING_GET,
                userId,
                context.chatId(),
                postId
        );
    }

    private SendMessage alreadyHaveActiveRespMessage(CallbackQueryContext context) {
        return ResponseBuilder.sendMessage(context.chatId())
                .text("Ви вже відгукнулися на це оголошення!")
                .keyboard(InlineKeyboardBuilder.builder()
                        .backButtonTo(CallbackId.FOSTERING)
                        .build())
                .build();
    }
}
