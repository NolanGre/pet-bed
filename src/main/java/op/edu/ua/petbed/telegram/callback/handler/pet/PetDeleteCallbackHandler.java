package op.edu.ua.petbed.telegram.callback.handler.pet;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.common.exceptions.PetBedException;
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

@NullMarked
@Component
@RequiredArgsConstructor
public class PetDeleteCallbackHandler implements CallbackHandler {

    private final TelegramMessageService messageService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.PET_DELETE;
    }

    @Override
    public PartialBotApiMethod<?> handle(CallbackQueryContext context) {
        Long entityId = context.callbackData().entityId();
        if (entityId == null) {
            throw new PetBedException("Entity ID is required for PET_DELETE", PetBedException.ErrorCode.INVALID_CALLBACK);
        }

        SendMessage message = ResponseBuilder.sendMessage(context.chatId())
                .text("⚠️ Ви впевнені, що хочете видалити анкету тварини? Цю дію неможливо скасувати.")
                .keyboard(InlineKeyboardBuilder.builder()
                        .navButtonsFor(getCallbackId(), entityId)
                        .backButtonFor(getCallbackId(), entityId)
                        .build())
                .build();

        messageService.editOrReplace(context.messageId(), message);

        return AnswerCallbackQuery.builder()
                .callbackQueryId(context.callbackQuery().getId())
                .build();
    }
}