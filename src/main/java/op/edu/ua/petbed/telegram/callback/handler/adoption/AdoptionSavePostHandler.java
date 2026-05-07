package op.edu.ua.petbed.telegram.callback.handler.adoption;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.adoption.AdoptionSavedPostService;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;

/**
 * Handler for ADOPTION_SAVE_POST callback - saves the current post.
 */
@NullMarked
@Component
@RequiredArgsConstructor
public class AdoptionSavePostHandler implements CallbackHandler {

    private final AdoptionSavedPostService adoptionSavedPostService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.ADOPTION_SAVE_POST;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        Long postId = context.callbackData().entityId();
        if (postId == null) {
            return AnswerCallbackQuery.builder()
                    .callbackQueryId(context.callbackQuery().getId())
                    .text("❌ Помилка: ID оголошення не вказано")
                    .showAlert(true)
                    .build();
        }

        Long userId = context.auth().userInternalId();

        adoptionSavedPostService.save(postId, userId);

        return AnswerCallbackQuery.builder()
                .callbackQueryId(context.callbackQuery().getId())
                .text("❤️ Оголошення збережено!")
                .build();
    }
}
