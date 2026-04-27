package op.edu.ua.petbed.telegram.callback.handler.feed;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.feed.FeedService;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import op.edu.ua.petbed.telegram.service.TelegramMessageService;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@NullMarked
@Component
@RequiredArgsConstructor
public class FeedPostDeleteCallbackHandler implements CallbackHandler {

    private final TelegramMessageService messageService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.FEED_POST_DELETE;
    }

    @Override
    public PartialBotApiMethod<?> handle(CallbackQueryContext context) {
        Long entityId = context.callbackData().entityId();
        if (entityId == null) {
            throw new PetBedException("Entity ID is required for FEED_POST_DELETE_CONFIRM", PetBedException.ErrorCode.INVALID_CALLBACK);
        }

        SendMessage message = ResponseBuilder.sendMessage(context.chatId())
                .text("⚠️ Ви впевнені, що хочете видалити цей пост? Цю дію неможливо скасувати.")
                .keyboard(InlineKeyboardBuilder.builder()
                        .navButtonsFor(getCallbackId(), entityId)
                        .backButtonFor(getCallbackId(), entityId)
                        .build())
                .build();

        return messageService.editOrReplace(context, message);
    }
}