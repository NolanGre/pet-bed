package op.edu.ua.petbed.telegram.callback.handler.fostering;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.fostering.FosteringSavedPostService;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;

@NullMarked
@Component
@RequiredArgsConstructor
public class FosteringSavePostHandler implements CallbackHandler {

    private final FosteringSavedPostService fosteringSavedPostService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.FOSTERING_SAVE_POST;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        Long postId = context.callbackData().entityId();
        if (postId == null) {
            throw new PetBedException("Post ID is required", PetBedException.ErrorCode.INVALID_CALLBACK);
        }
        Long userId = context.auth().userInternalId();

        fosteringSavedPostService.save(postId, userId);

        return AnswerCallbackQuery.builder()
                .callbackQueryId(context.callbackQuery().getId())
                .text("❤️ Оголошення збережено!")
                .build();
    }
}
