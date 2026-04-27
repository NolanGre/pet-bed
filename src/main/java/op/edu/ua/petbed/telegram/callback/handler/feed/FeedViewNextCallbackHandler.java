package op.edu.ua.petbed.telegram.callback.handler.feed;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.feed.FeedService;
import op.edu.ua.petbed.common.dto.FeedPostDTO;
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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@NullMarked
@Component
@RequiredArgsConstructor
public class FeedViewNextCallbackHandler implements CallbackHandler {

    private final FeedService feedService;
    private final TelegramMessageService messageService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.FEED_VIEW_NEXT;
    }

    @Override
    public PartialBotApiMethod<?> handle(CallbackQueryContext context) {
        Long userId = context.auth().userInternalId();
        Integer messageId = context.messageId();
        Long chatId = context.chatId();

        FeedPostDTO post = feedService.findNextPostAndMarkAsViewed(userId);
        if (post == null) {
            return noMorePostsMessage(chatId, messageId, context.callbackQuery().getId());
        }

        return mapToResponse(context, post, messageId);
    }

    private PartialBotApiMethod<?> mapToResponse(CallbackQueryContext context, FeedPostDTO post, Integer messageId) {
        ResponseBuilder.editMessage(context.chatId(), messageId)
                .text("")
                .build();

        return ResponseBuilder.sendPhoto(context.chatId(), post.photoUrl())
                .caption(formatPostInfo(post))
                .keyboard(buildKeyboard())
                .build();
    }

    private PartialBotApiMethod<?> noMorePostsMessage(Long chatId, Integer messageId, String callbackQueryId) {
        messageService.editOrReplace(messageId, ResponseBuilder.sendMessage(chatId)
                .text("📭 Більше публікацій немає")
                .keyboard(InlineKeyboardBuilder.builder()
                        .backButtonFor(CallbackId.FEED)
                        .build())
                .build());

        return AnswerCallbackQuery.builder()
                .callbackQueryId(callbackQueryId)
                .build();
    }

    private InlineKeyboardMarkup buildKeyboard() {
        return InlineKeyboardBuilder.builder()
                .navButtonsFor(CallbackId.FEED_VIEW)
                .backButtonFor(CallbackId.FEED)
                .build();
    }

    private String formatPostInfo(FeedPostDTO post) {
        StringBuilder sb = new StringBuilder();
        sb.append(post.text()).append("\n\n");

        if (post.distance() != null) {
            sb.append(formatDistance(post.distance())).append("\n");
        }

        sb.append("👤 @").append(post.publisherUsername());

        return sb.toString();
    }

    private String formatDistance(double distanceMeters) {
        if (distanceMeters < 1000) {
            return "📍 " + (int) distanceMeters + "м від вас";
        } else {
            double km = distanceMeters / 1000;
            return String.format("📍 %.1f км від вас", km);
        }
    }
}