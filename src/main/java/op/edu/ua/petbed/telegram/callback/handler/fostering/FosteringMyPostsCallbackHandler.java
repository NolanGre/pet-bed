package op.edu.ua.petbed.telegram.callback.handler.fostering;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.fostering.FosteringPostService;
import op.edu.ua.petbed.common.dto.FosteringPostDTO;
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
 * Handler for FOSTERING_MY_POSTS callback - shows list of owner's fostering posts.
 */
@NullMarked
@Component
@RequiredArgsConstructor
@Slf4j
public class FosteringMyPostsCallbackHandler implements CallbackHandler {

    private final FosteringPostService fosteringPostService;
    private final TelegramMessageService messageService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.FOSTERING_MY_POSTS;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        Long userId = context.auth().userInternalId();
        var callbackData = context.callbackData();

        int offset = callbackData.offset() != null ? callbackData.offset() : 0;
        Page<FosteringPostDTO> posts = fosteringPostService.findAllByOwnerId(userId,
                PageRequest.of(offset, KeyboardLayout.DEFAULT.pageSize()));

        if (posts.isEmpty()) {
            return noPostsExistMessage(context);
        }

        SendMessage message = ResponseBuilder.sendMessage(context.chatId())
                .text("📋 Ваші оголошення про перетримку:")
                .keyboard(buildKeyboard(context, posts))
                .build();

        return messageService.editOrReplace(context, message);
    }

    private InlineKeyboardMarkup buildKeyboard(CallbackQueryContext context, Page<FosteringPostDTO> posts) {
        return InlineKeyboardBuilder.builder()
                .paginatedList(toPageDto(posts), context.callbackData())
                .backButtonTo(CallbackId.FOSTERING)
                .build();
    }

    private Page<CallbackListItem> toPageDto(Page<FosteringPostDTO> posts) {
        return posts.map(post -> new CallbackListItem(
                CallbackId.FOSTERING_POST_DETAIL,
                post.id(),
                getStatusEmoji(post.status()) + " " + post.petName()
        ));
    }

    private SendMessage noPostsExistMessage(CallbackQueryContext context) {
        return ResponseBuilder.sendMessage(context.chatId())
                .text("""
                        📋 У вас ще немає оголошень про перетримку тварин.

                        Створіть перше оголошення!
                        """)
                .keyboard(InlineKeyboardBuilder.builder()
                        .navButtonsFor(CallbackId.FOSTERING_MY_POSTS)
                        .backButtonTo(CallbackId.FOSTERING)
                        .build())
                .build();
    }

    private String getStatusEmoji(op.edu.ua.petbed.fostering.domain.model.FosteringPostStatus status) {
        return switch (status) {
            case ACTIVE -> "🟢";
            case PENDING_CONFIRMATION -> "⏳";
            case COMPLETED -> "✅";
            case CANCELLED -> "🚫";
        };
    }
}
