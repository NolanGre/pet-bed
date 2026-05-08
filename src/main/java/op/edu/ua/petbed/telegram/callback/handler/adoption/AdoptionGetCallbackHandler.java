package op.edu.ua.petbed.telegram.callback.handler.adoption;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.adoption.AdoptionPostService;
import op.edu.ua.petbed.common.dto.AdoptionRecommendationDTO;

import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

/**
 * Handler for ADOPTION_GET callback - shows adoption posts in the feed with pagination via offset.
 */
@NullMarked
@Component
@RequiredArgsConstructor
public class AdoptionGetCallbackHandler implements CallbackHandler {

    private final AdoptionPostService adoptionPostService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.ADOPTION_GET;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        Long userId = context.auth().userInternalId();
        var callbackData = context.callbackData();
        int offset = callbackData.offset() != null ? callbackData.offset() : 0;

        if (offset == 0) {
            adoptionPostService.resetOffset(userId);
        }

        AdoptionRecommendationDTO post = adoptionPostService.findNextForFeed(userId, offset);

        if (post == null) {
            return noPostsExistMessage(context);
        }

        adoptionPostService.recordView(post.postId(), userId);

        return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                .text(formatPostInfo(post))
                .keyboard(buildKeyboard(post, offset))
                .build();
    }

    private EditMessageText noPostsExistMessage(CallbackQueryContext context) {
        return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                .text("""
                        📭 Немає доступних оголошень.

                        Зараз немає тварин для адопції.
                        Перевірте пізніше!
                        """)
                .keyboard(InlineKeyboardBuilder.builder()
                        .backButtonFor(CallbackId.ADOPTION)
                        .build())
                .build();
    }

    private String formatPostInfo(AdoptionRecommendationDTO post) {
        StringBuilder text = new StringBuilder();
        text.append("🐾 ").append(post.petName()).append("\n\n");

        if (!post.petBreed().isBlank()) {
            text.append("🏷️ Порода: ").append(post.petBreed()).append("\n");
        }
        if (!post.petColor().isBlank()) {
            text.append("🎨 Колір: ").append(post.petColor()).append("\n");
        }
        if (post.ownerComment() != null && !post.ownerComment().isBlank()) {
            text.append("\n💬 ").append(post.ownerComment()).append("\n");
        }
        return text.toString();
    }

    private InlineKeyboardMarkup buildKeyboard(AdoptionRecommendationDTO post, int offset) {
        return InlineKeyboardBuilder.builder()
                .navButtonsFor(CallbackId.ADOPTION_GET, post.postId(), child -> {
                    if (child == CallbackId.ADOPTION_SAVE_POST) return !post.isSaved();
                    if (child == CallbackId.ADOPTION_UNSAVE_POST) return post.isSaved();
                    if (child == CallbackId.ADOPTION_GET_NEXT) return true;
                    return false;
                })
                .backButtonFor(CallbackId.ADOPTION)
                .build();
    }
}
