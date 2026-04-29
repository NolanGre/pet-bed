package op.edu.ua.petbed.telegram.callback.handler.lost;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.common.dto.LostRequestDTO;
import op.edu.ua.petbed.common.dto.PetDTO;
import op.edu.ua.petbed.lost.LostRequestService;
import op.edu.ua.petbed.pet.PetService;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import op.edu.ua.petbed.telegram.service.TelegramMessageService;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

/**
 * Handler for LOST_FINISH callback - confirmation step before canceling a lost search.
 * Shows warning message and asks for confirmation.
 */
@NullMarked
@Component
@RequiredArgsConstructor
public class LostFinishCallbackHandler implements CallbackHandler {

    private final LostRequestService lostRequestService;
    private final PetService petService;
    private final TelegramMessageService telegramMessageService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.LOST_FINISH;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        Long lostRequestId = context.callbackData().entityId();
        if (lostRequestId == null) {
            throw new IllegalArgumentException("Entity ID is required for LOST_FINISH");
        }

        LostRequestDTO lostRequest = lostRequestService.findById(lostRequestId);
        PetDTO pet = petService.findById(lostRequest.petId());

        String messageText = String.format("""
                ⚠️ Завершення пошуку
                
                Ви впевнені, що хочете завершити пошук для %s?
                
                ⚡ Ця дія:
                • Скасує активний пошук
                • Видалить всі рекомендації
                • Поверне тварину у звичайний статус
                
                Ви зможете розпочати новий пошук у будь-який момент.
                """, pet.name());

        SendMessage message = ResponseBuilder.sendMessage(context.chatId())
                .text(messageText)
                .keyboard(buildKeyboard(lostRequestId))
                .build();

        return telegramMessageService.editOrSend(context, message);
    }

    private InlineKeyboardMarkup buildKeyboard(Long lostRequestId) {
        return InlineKeyboardBuilder.builder()
                .addButton("✅ Підтвердити", CallbackId.LOST_FINISH_CONFIRM, lostRequestId)
                .backButtonTo(CallbackId.LOST_ACTIVE_DETAIL, lostRequestId)
                .build();
    }
}
