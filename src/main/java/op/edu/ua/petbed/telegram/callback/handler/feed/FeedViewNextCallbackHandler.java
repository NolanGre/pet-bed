package op.edu.ua.petbed.telegram.callback.handler.feed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.common.dto.FeedPostDTO;
import op.edu.ua.petbed.feed.FeedService;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Slf4j
@NullMarked
@Component
@RequiredArgsConstructor
public class FeedViewNextCallbackHandler implements CallbackHandler {

    private final FeedService feedService;
    private final TelegramClient client;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.FEED_VIEW_NEXT;
    }

    @Override
    public PartialBotApiMethod<?> handle(CallbackQueryContext context) {
        Long userId = context.auth().userInternalId();

        tryToDeleteKeyboardInMessage(context);

        FeedPostDTO post = feedService.findNextPostAndMarkAsViewed(userId);
        if (post == null) {
            return noMorePostsMessage(context.chatId());
        }

        return mapToResponse(context, post, context.messageId());
    }

    private void tryToDeleteKeyboardInMessage(CallbackQueryContext context) {
        try {
            client.execute(EditMessageReplyMarkup.builder()
                    .chatId(context.chatId())
                    .messageId(context.messageId())
                    .replyMarkup(InlineKeyboardMarkup.builder().build())
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to remove keyboard from message {}: {}", context.messageId(), e.getMessage(), e);
        }
    }

    private PartialBotApiMethod<?> mapToResponse(CallbackQueryContext context, FeedPostDTO post, Integer messageId) {
        return ResponseBuilder.sendPhoto(context.chatId(), post.photoUrl())
                .caption(formatPostInfo(post))
                .keyboard(InlineKeyboardBuilder.builder()
                        .navButtonsFor(CallbackId.FEED_VIEW)
                        .backButtonTo(CallbackId.FEED)
                        .build())
                .build();
    }

    private BotApiMethod<?> noMorePostsMessage(Long chatId) {
        return ResponseBuilder.sendMessage(chatId)
                .text("📭 Більше публікацій немає")
                .keyboard(InlineKeyboardBuilder.builder()
                        .backButtonTo(CallbackId.FEED)
                        .build())
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