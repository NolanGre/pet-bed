package op.edu.ua.petbed.telegram.callback.handler.adoption;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.adoption.AdoptionResponseService;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;

/**
 * Handler for ADOPTION_POST_RESPONSE_RESTORE - actually restores the response.
 */
@NullMarked
@Component
@RequiredArgsConstructor
public class AdoptionResponseRestoreActionHandler implements CallbackHandler {

    private final AdoptionResponseService adoptionResponseService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.ADOPTION_POST_RESPONSE_RESTORE;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        Long responseId = context.callbackData().entityId();
if (responseId == null) {
            return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                    .text("❌ Помилка: ID відгуку не вказано")
                    .keyboard(InlineKeyboardBuilder.builder()
                            .backButtonFor(CallbackId.ADOPTION_MY_POSTS)
                            .build())
                    .build();
        }

        Long ownerId = context.auth().userInternalId();

        adoptionResponseService.restore(responseId, ownerId);

        return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                .text("🔄 Відгук відновлено. Користувач отримає повідомлення.")
                .keyboard(InlineKeyboardBuilder.builder()
                        .backButtonTo(CallbackId.ADOPTION_POST_RESPONSE_DETAIL)
                        .build())
                .build();
    }
}