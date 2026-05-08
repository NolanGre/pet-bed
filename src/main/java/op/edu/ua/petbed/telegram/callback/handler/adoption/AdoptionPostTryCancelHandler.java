package op.edu.ua.petbed.telegram.callback.handler.adoption;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.adoption.AdoptionPostService;
import op.edu.ua.petbed.common.dto.AdoptionPostDTO;

import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;

/**
 * Handler for ADOPTION_POST_TRY_DELETE - shows cancel confirmation dialog.
 */
@NullMarked
@Component
@RequiredArgsConstructor
public class AdoptionPostTryCancelHandler implements CallbackHandler {

    private final AdoptionPostService adoptionPostService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.ADOPTION_POST_TRY_DELETE;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        Long postId = context.callbackData().entityId();
        if (postId == null) {
            return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                    .text("❌ Помилка: ID оголошення не вказано")
                    .keyboard(InlineKeyboardBuilder.builder()
                            .backButtonTo(CallbackId.ADOPTION_POST_DETAIL)
                            .build())
                    .build();
        }

        AdoptionPostDTO post = adoptionPostService.findById(postId).post();

        return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                .text("""
                        ⚠️ Скасувати оголошення?
                        
                        Тварина: %s
                        
                        При скасуванні:
                        - Оголошення буде закрито
                        - Всі відгуки будуть відхилені
                        - Тварина повернеться в звичайний статус
                        """.formatted(post.petName()))
                .keyboard(InlineKeyboardBuilder.builder()
                        .addButton("❌ Так, скасувати",
                                CallbackId.ADOPTION_POST_DELETE, postId)
                        .backButtonTo(CallbackId.ADOPTION_POST_DETAIL)
                        .build())
                .build();
    }
}
