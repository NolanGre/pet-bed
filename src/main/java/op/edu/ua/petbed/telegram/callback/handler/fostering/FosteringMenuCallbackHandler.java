package op.edu.ua.petbed.telegram.callback.handler.fostering;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.fostering.FosteringPostService;
import op.edu.ua.petbed.fostering.FosteringResponseService;
import op.edu.ua.petbed.fostering.FosteringSavedPostService;
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
 * Handler for FOSTERING callback - shows the main fostering menu.
 */
@NullMarked
@Component
@RequiredArgsConstructor
@Slf4j
public class FosteringMenuCallbackHandler implements CallbackHandler {

    private final FosteringPostService fosteringPostService;
    private final FosteringResponseService fosteringResponseService;
    private final FosteringSavedPostService fosteringSavedPostService;
    private final TelegramMessageService messageService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.FOSTERING;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        Long userId = context.auth().userInternalId();

        boolean hasMyPosts = fosteringPostService.existsByOwnerId(userId);
        boolean hasMyResponses = fosteringResponseService.existsByResponderId(userId);
        boolean hasMySaved = fosteringSavedPostService.existsByUserId(userId);

        SendMessage message = ResponseBuilder.sendMessage(context.chatId())
                .text("⏳ Перетримка\n\nТут ви можете запропонувати тварину на перетримку або допомогти комусь з перетримкою.")
                .keyboard(buildKeyboard(hasMyPosts, hasMyResponses, hasMySaved))
                .build();

        return messageService.editOrReplace(context, message);
    }

    private InlineKeyboardMarkup buildKeyboard(boolean hasMyPosts, boolean hasMyResponses, boolean hasMySaved) {
        var builder = InlineKeyboardBuilder.builder()
                .navButtonsFor(CallbackId.FOSTERING, child -> {
                    if (child == CallbackId.FOSTERING_MY_POSTS) {
                        return hasMyPosts;
                    }
                    if (child == CallbackId.FOSTERING_MY_RESPONSES) {
                        return hasMyResponses;
                    }
                    return true;
                })
                .backButtonFor(CallbackId.FOSTERING);

        return builder.build();
    }
}