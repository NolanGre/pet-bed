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
 * Handler for LOST_FINISH_CONFIRM callback - confirms and cancels the lost search.
 * Calls LostRequestService.cancel() and returns to the active searches list.
 */
@NullMarked
@Component
@RequiredArgsConstructor
public class LostFinishConfirmCallbackHandler implements CallbackHandler {

    private final LostRequestService lostRequestService;
    private final PetService petService;
    private final TelegramMessageService telegramMessageService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.LOST_FINISH_CONFIRM;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        Long lostRequestId = context.callbackData().entityId();
        if (lostRequestId == null) {
            throw new IllegalArgumentException("Entity ID is required for LOST_FINISH_CONFIRM");
        }

        LostRequestDTO lostRequest = lostRequestService.findById(lostRequestId);
        PetDTO pet = petService.findById(lostRequest.petId());

        // Cancel the search
        lostRequestService.cancel(lostRequestId);

        String messageText = String.format("""
                ✅ Пошук завершено
                
                Пошук для %s успішно скасовано.
                
                Тварина повернута у звичайний статус.
                Ви можете розпочати новий пошук у будь-який момент.
                """, pet.name());

        SendMessage message = ResponseBuilder.sendMessage(context.chatId())
                .text(messageText)
                .keyboard(buildKeyboard())
                .build();

        return telegramMessageService.editOrSend(context, message);
    }

    private InlineKeyboardMarkup buildKeyboard() {
        return InlineKeyboardBuilder.builder()
                .addButton("📋 Мої пошуки", CallbackId.LOST_ACTIVE)
                .addButton("🔎 Почати пошук", CallbackId.LOST_START)
                .backButtonTo(CallbackId.LOST)
                .build();
    }
}
