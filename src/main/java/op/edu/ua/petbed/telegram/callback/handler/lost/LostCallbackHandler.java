package op.edu.ua.petbed.telegram.callback.handler.lost;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.lost.LostRequestService;
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
 * Handler for LOST callback - displays the Lost & Found module main menu.
 * Shows options to:
 * - Start a new search (LOST_START)
 * - Report a found pet (LOST_FOUND)
 * - View active searches (LOST_ACTIVE) - only if user has active searches
 */
@NullMarked
@Component
@RequiredArgsConstructor
public class LostCallbackHandler implements CallbackHandler {

    private final LostRequestService lostRequestService;
    private final TelegramMessageService telegramMessageService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.LOST;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        return mapToResponse(context);
    }

    private BotApiMethod<?> mapToResponse(CallbackQueryContext context) {
        var auth = context.auth();
        String messageText = """
                🔍 Пошук тварин
                
                Тут ви можете:
                • Запустити пошук загубленої тварини
                • Повідомити про знайдену тварину
                • Переглянути активні пошуки
                """;

        SendMessage response = ResponseBuilder.sendMessage(context.chatId())
                .text(messageText)
                .keyboard(lostKeyboard(auth.userInternalId()))
                .build();

        return telegramMessageService.editOrSend(context, response);
    }

    private InlineKeyboardMarkup lostKeyboard(Long userId) {
        boolean hasActiveSearches = !lostRequestService.findActiveByOwnerId(userId).isEmpty();

        return InlineKeyboardBuilder.builder()
                .navButtonsFor(CallbackId.LOST, child -> {
                    if (child == CallbackId.LOST_ACTIVE) {
                        return hasActiveSearches;
                    }
                    return true;
                })
                .backButtonFor(CallbackId.LOST)
                .build();
    }
}
