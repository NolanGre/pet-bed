package op.edu.ua.petbed.telegram.callback.handler.adoption;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.adoption.AdoptionSavedPostService;
import op.edu.ua.petbed.telegram.callback.CallbackData;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;

/**
 * Handler for ADOPTION_UNSAVE_POST callback - removes the post from saved.
 */
@NullMarked
@Component
@RequiredArgsConstructor
public class AdoptionUnsavePostHandler implements CallbackHandler {

    private final AdoptionSavedPostService adoptionSavedPostService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.ADOPTION_UNSAVE_POST;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        Long postId = context.callbackData().entityId();
        if (postId == null) {
            return ResponseBuilder.answerCallbackQuery(context.callbackQuery().getId())
                    .text("❌ Помилка: ID оголошення не вказано")
                    .showAlert(true)
                    .build();
        }

        Long userId = context.auth().userInternalId();

        adoptionSavedPostService.unsave(postId, userId);

        return ResponseBuilder.answerCallbackQuery(context.callbackQuery().getId())
                .text("💔 Оголошення видалено зі збережених")
                .build();
    }
}
