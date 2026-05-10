package op.edu.ua.petbed.telegram.callback.handler.fostering;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.fostering.FosteringPostService;
import op.edu.ua.petbed.common.dto.FosteringPostDTO;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;

/**
 * Handler for FOSTERING_POST_TRY_DELETE - shows delete confirmation dialog.
 */
@NullMarked
@Component
@RequiredArgsConstructor
@Slf4j
public class FosteringPostTryDeleteHandler implements CallbackHandler {

    private final FosteringPostService fosteringPostService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.FOSTERING_POST_TRY_DELETE;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        Long postId = context.callbackData().entityId();
        if (postId == null) {
            return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                    .text("❌ Помилка: ID оголошення не вказано")
                    .keyboard(InlineKeyboardBuilder.builder()
                            .backButtonTo(CallbackId.FOSTERING_MY_POSTS)
                            .build())
                    .build();
        }

        FosteringPostDTO post = fosteringPostService.findById(postId).post();

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
                                CallbackId.FOSTERING_POST_DELETE, postId)
                        .backButtonTo(CallbackId.FOSTERING_POST_DETAIL, postId)
                        .build())
                .build();
    }
}
