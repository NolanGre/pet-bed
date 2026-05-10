package op.edu.ua.petbed.telegram.callback.handler.fostering;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.fostering.FosteringSavedPostService;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import op.edu.ua.petbed.telegram.service.TelegramMessageService;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

/**
 * Handler for FOSTERING_UNSAVE_POST callback - removes the post from saved.
 */
@NullMarked
@Component
@RequiredArgsConstructor
public class FosteringUnsavePostHandler implements CallbackHandler {

    private final FosteringSavedPostService fosteringSavedPostService;
    private final TelegramMessageService telegramMessageService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.FOSTERING_UNSAVE_POST;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        Long postId = context.callbackData().entityId();
        if (postId == null) {
            throw new IllegalArgumentException("Post ID is required");
        }

        Long userId = context.auth().userInternalId();

        fosteringSavedPostService.unsave(postId, userId);

        return telegramMessageService.editOrReplace(context, message(context));
    }

    private SendMessage message(CallbackQueryContext context) {
        return ResponseBuilder.sendMessage(context.chatId())
                .text("💔 Оголошення видалено зі збережених")
                .keyboard(InlineKeyboardBuilder.builder()
                        .backButtonTo(CallbackId.FOSTERING)
                        .build())
                .build();
    }
}
