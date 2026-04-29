package op.edu.ua.petbed.telegram.callback.handler.lost;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.common.dto.LostRequestDTO;
import op.edu.ua.petbed.lost.LostRequestService;
import op.edu.ua.petbed.pet.PetService;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.response.CallbackListItem;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.KeyboardLayout;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import op.edu.ua.petbed.telegram.service.TelegramMessageService;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

/**
 * Handler for LOST_ACTIVE callback - displays paginated list of user's active lost searches.
 * Shows pets that are currently in search status with ability to view details.
 */
@NullMarked
@Component
@RequiredArgsConstructor
public class LostActiveCallbackHandler implements CallbackHandler {

    private final LostRequestService lostRequestService;
    private final PetService petService;
    private final TelegramMessageService telegramMessageService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.LOST_ACTIVE;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        var callbackData = context.callbackData();
        int offset = callbackData.offset() != null ? callbackData.offset() : 0;

        Page<LostRequestDTO> lostRequests = lostRequestService.findByOwnerId(
                context.auth().userInternalId(),
                PageRequest.of(offset, KeyboardLayout.DEFAULT.pageSize())
        );

        if (lostRequests.isEmpty()) {
            SendMessage message = ResponseBuilder.sendMessage(context.chatId())
                    .text("""
                            📋 У вас немає активних пошуків.
                            
                            Натисніть "Почати пошук" щоб розпочати пошук загубленої тварини.
                            """)
                    .keyboard(InlineKeyboardBuilder.builder()
                            .addButton("🔎 Почати пошук", CallbackId.LOST_START)
                            .backButtonTo(CallbackId.LOST)
                            .build())
                    .build();
            return telegramMessageService.editOrSend(context, message);
        }

        SendMessage message = ResponseBuilder.sendMessage(context.chatId())
                .text("📋 Ваші активні пошуки:")
                .keyboard(buildKeyboard(context, lostRequests))
                .build();

        return telegramMessageService.editOrSend(context, message);
    }

    private InlineKeyboardMarkup buildKeyboard(CallbackQueryContext context, Page<LostRequestDTO> lostRequests) {
        return InlineKeyboardBuilder.builder()
                .paginatedList(toPageDto(lostRequests), context.callbackData())
                .backButtonTo(CallbackId.LOST)
                .build();
    }

    private Page<CallbackListItem> toPageDto(Page<LostRequestDTO> lostRequests) {
        return lostRequests.map(lr -> {
            String petName = petService.findById(lr.petId()).name();
            String label = "🔍 " + petName;
            return new CallbackListItem(CallbackId.LOST_ACTIVE_DETAIL, lr.id(), label);
        });
    }
}
