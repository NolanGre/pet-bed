package op.edu.ua.petbed.telegram.callback.handler.adoption;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.adoption.AdoptionPostService;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;

/**
 * Handler for ADOPTION_POST_DELETE - actually cancels the adoption post.
 */
@NullMarked
@Component
@RequiredArgsConstructor
public class AdoptionPostCancelActionHandler implements CallbackHandler {

    private final AdoptionPostService adoptionPostService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.ADOPTION_POST_DELETE;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        Long postId = context.callbackData().entityId();
        if (postId == null) {
            return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                    .text("❌ Помилка: ID оголошення не вказано")
                    .keyboard(InlineKeyboardBuilder.builder()
                            .backButtonTo(CallbackId.ADOPTION_MY_POSTS)
                            .build())
                    .build();
        }

        Long ownerId = context.auth().userInternalId();

        adoptionPostService.cancel(postId, ownerId);

        return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                .text("""
                        ✅ Оголошення скасовано.
                        
                        Тварина повернулась у звичайний статус.
                        Ви можете створити нове оголошення будь-коли.
                        """)
                .keyboard(InlineKeyboardBuilder.builder()
                        .backButtonTo(CallbackId.ADOPTION_MY_POSTS)
                        .build())
                .build();
    }
}
