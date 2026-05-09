package op.edu.ua.petbed.telegram.callback.handler.adoption;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.adoption.AdoptionSavedPostService;
import op.edu.ua.petbed.common.dto.AdoptionPostDTO;
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
 * Handler for ADOPTION_MY_SAVED callback - shows list of user's saved adoption posts.
 */
@NullMarked
@Component
@RequiredArgsConstructor
public class AdoptionMySavedCallbackHandler implements CallbackHandler {

    private final AdoptionSavedPostService adoptionSavedPostService;
    private final TelegramMessageService messageService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.ADOPTION_MY_SAVED;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        Long userId = context.auth().userInternalId();
        var callbackData = context.callbackData();

        int offset = callbackData.offset() != null ? callbackData.offset() : 0;
        Page<AdoptionPostDTO> posts = adoptionSavedPostService.findSavedByUserId(userId,
                PageRequest.of(offset, KeyboardLayout.DEFAULT.pageSize()));

        if (posts.isEmpty()) {
            return noSavedPostsMessage(context);
        }

        SendMessage message = ResponseBuilder.sendMessage(context.chatId())
                .text("❤️ Ваші збережені оголошення:")
                .keyboard(buildKeyboard(context, posts))
                .build();

        return messageService.editOrReplace(context, message);
    }

    private InlineKeyboardMarkup buildKeyboard(CallbackQueryContext context, Page<AdoptionPostDTO> posts) {
        return InlineKeyboardBuilder.builder()
                .paginatedList(toPageDto(posts), context.callbackData())
                .backButtonTo(CallbackId.ADOPTION)
                .build();
    }

    private Page<CallbackListItem> toPageDto(Page<AdoptionPostDTO> posts) {
        return posts.map(post -> new CallbackListItem(
                CallbackId.ADOPTION_SAVED_DETAIL,
                post.id(),
                "❤️ " + post.petName()
        ));
    }

    private SendMessage noSavedPostsMessage(CallbackQueryContext context) {
        return ResponseBuilder.sendMessage(context.chatId())
                .text("""
                        ❤️ У вас немає збережених оголошень.

                        Переглядайте оголошення в розділі "Отримати тварину"
                        і зберігайте ті, що вас зацікавили.
                        """)
                .keyboard(InlineKeyboardBuilder.builder()
                        .addButton("🔍 Переглянути оголошення", CallbackId.ADOPTION_GET)
                        .backButtonTo(CallbackId.ADOPTION)
                        .build())
                .build();
    }
}