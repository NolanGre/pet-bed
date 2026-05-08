package op.edu.ua.petbed.telegram.callback.handler.adoption;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.adoption.AdoptionPostService;
import op.edu.ua.petbed.adoption.AdoptionResponseService;
import op.edu.ua.petbed.adoption.AdoptionSavedPostService;
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
 * Handler for ADOPTION callback - shows the main adoption menu.
 */
@NullMarked
@Component
@RequiredArgsConstructor
public class AdoptionMenuCallbackHandler implements CallbackHandler {

    private final AdoptionPostService adoptionPostService;
    private final AdoptionResponseService adoptionResponseService;
    private final AdoptionSavedPostService adoptionSavedPostService;
    private final TelegramMessageService messageService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.ADOPTION;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        Long userId = context.auth().userInternalId();

        boolean hasMyPosts = adoptionPostService.existsByOwnerId(userId);
        boolean hasMyResponses = adoptionResponseService.existsByResponderId(userId);
        boolean hasMySaved = adoptionSavedPostService.existsByUserId(userId);

        SendMessage message = ResponseBuilder.sendMessage(context.chatId())
                .text("🏠 Адопція\n\nТут ви можете віддати тварину або знайти собі нового друга.")
                .keyboard(buildKeyboard(hasMyPosts, hasMyResponses, hasMySaved))
                .build();

        return messageService.editOrReplace(context, message);
    }

    private InlineKeyboardMarkup buildKeyboard(boolean hasMyPosts, boolean hasMyResponses, boolean hasMySaved) {
        var builder = InlineKeyboardBuilder.builder()
                .navButtonsFor(CallbackId.ADOPTION, child -> {
                    if (child == CallbackId.ADOPTION_MY_POSTS) {
                        return hasMyPosts;
                    }
                    if (child == CallbackId.ADOPTION_MY_RESPONSES) {
                        return hasMyResponses;
                    }
                    if (child == CallbackId.ADOPTION_MY_SAVED) {
                        return hasMySaved;
                    }
                    return true;
                })
                .backButtonFor(CallbackId.ADOPTION);

        return builder.build();
    }
}
