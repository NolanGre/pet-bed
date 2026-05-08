package op.edu.ua.petbed.telegram.callback.handler.adoption;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.adoption.AdoptionPostService;
import op.edu.ua.petbed.common.dto.AdoptionPostDTO;
import op.edu.ua.petbed.common.model.AdoptionPostStatus;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.response.CallbackListItem;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.KeyboardLayout;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

/**
 * Handler for ADOPTION_MY_POSTS callback - shows list of owner's adoption posts.
 */
@NullMarked
@Component
@RequiredArgsConstructor
public class AdoptionMyPostsCallbackHandler implements CallbackHandler {

    private final AdoptionPostService adoptionPostService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.ADOPTION_MY_POSTS;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        Long userId = context.auth().userInternalId();
        var callbackData = context.callbackData();

        int offset = callbackData.offset() != null ? callbackData.offset() : 0;
        Page<AdoptionPostDTO> posts = adoptionPostService.findAllByOwnerId(userId,
                PageRequest.of(offset, KeyboardLayout.DEFAULT.pageSize()));

        if (posts.isEmpty()) {
            return noPostsExistMessage(context);
        }

        return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                .text("📋 Ваші оголошення про передачу:")
                .keyboard(buildKeyboard(context, posts))
                .build();
    }

    private InlineKeyboardMarkup buildKeyboard(CallbackQueryContext context, Page<AdoptionPostDTO> posts) {
        return InlineKeyboardBuilder.builder()
                .paginatedList(toPageDto(posts), context.callbackData())
                .backButtonFor(getCallbackId())
                .build();
    }

    private Page<CallbackListItem> toPageDto(Page<AdoptionPostDTO> posts) {
        return posts.map(post -> new CallbackListItem(
                CallbackId.ADOPTION_POST_DETAIL,
                post.id(),
                getStatusEmoji(post.status()) + " " + post.petName()
        ));
    }

    private EditMessageText noPostsExistMessage(CallbackQueryContext context) {
        return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                .text("""
                        📋 У вас ще немає оголошень про передачу тварин.
                        
                        Створіть перше оголошення!
                        """)
                .keyboard(InlineKeyboardBuilder.builder()
                        .navButtonsFor(CallbackId.ADOPTION_MY_POSTS)
                        .backButtonFor(CallbackId.ADOPTION)
                        .build())
                .build();
    }

    private String getStatusEmoji(AdoptionPostStatus status) {
        return switch (status) {
            case ACTIVE -> "🟢";
            case PENDING_CONFIRMATION -> "⏳";
            case COMPLETED -> "✅";
            case CANCELLED -> "❌";
        };
    }
}
