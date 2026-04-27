package op.edu.ua.petbed.telegram.callback.handler.feed;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.feed.FeedService;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@NullMarked
@Component
@RequiredArgsConstructor
public class FeedPostDeleteConfirmCallbackHandler implements CallbackHandler {

    private final FeedService feedService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.FEED_POST_DELETE_CONFIRM;
    }

    @Override
    public PartialBotApiMethod<?> handle(CallbackQueryContext context) {
        Long entityId = context.callbackData().entityId();
        if (entityId == null) {
            throw new PetBedException("Entity ID is required for FEED_POST_DELETE_CONFIRM", PetBedException.ErrorCode.INVALID_CALLBACK);
        }

        Long userId = context.auth().userInternalId();
        feedService.delete(entityId, userId);

        var keyboard = getInlineKeyboardMarkup();

        return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                .text("✅ Оголошення видалено")
                .keyboard(keyboard)
                .build();
    }

    private InlineKeyboardMarkup getInlineKeyboardMarkup() {
        return InlineKeyboardBuilder.builder()
                .backButtonTo(CallbackId.FEED_MY_POSTS)
                .build();
    }
}