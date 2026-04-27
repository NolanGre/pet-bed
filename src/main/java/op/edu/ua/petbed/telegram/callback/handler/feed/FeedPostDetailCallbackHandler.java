package op.edu.ua.petbed.telegram.callback.handler.feed;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.feed.FeedService;
import op.edu.ua.petbed.common.dto.FeedPostDTO;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@NullMarked
@Component
@RequiredArgsConstructor
public class FeedPostDetailCallbackHandler implements CallbackHandler {

    private final FeedService feedService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.FEED_POST_DETAIL;
    }

    @Override
    public PartialBotApiMethod<?> handle(CallbackQueryContext context) {
        Long entityId = context.callbackData().entityId();
        if (entityId == null) {
            throw new PetBedException("Entity ID is required for FEED_POST_DETAIL",
                    PetBedException.ErrorCode.INVALID_CALLBACK);
        }

        FeedPostDTO post = feedService.findById(entityId);
        return mapToResponse(context, post);
    }

    private PartialBotApiMethod<?> mapToResponse(CallbackQueryContext context, FeedPostDTO post) {
        return ResponseBuilder.sendPhoto(context.chatId(), post.photoUrl())
                .caption(formatPostInfo(post))
                .keyboard(buildKeyboard(post.id()))
                .build();
    }

    private InlineKeyboardMarkup buildKeyboard(Long postId) {
        return InlineKeyboardBuilder.builder()
                .navButtonsFor(CallbackId.FEED_POST_DETAIL, postId)
                .backButtonFor(CallbackId.FEED_MY_POSTS)
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