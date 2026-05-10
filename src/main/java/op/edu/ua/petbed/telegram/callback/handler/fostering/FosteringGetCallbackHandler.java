package op.edu.ua.petbed.telegram.callback.handler.fostering;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.common.dto.FosteringRecommendationDTO;
import op.edu.ua.petbed.fostering.FosteringPostService;
import op.edu.ua.petbed.fostering.FosteringSavedPostService;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import op.edu.ua.petbed.user.UserService;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

/**
 * Handler for FOSTERING_GET callback - shows fostering posts in the feed.
 * Uses user offset for navigation (not callback offset).
 */
@NullMarked
@Component
@RequiredArgsConstructor
@Slf4j
public class FosteringGetCallbackHandler implements CallbackHandler {

    private final FosteringPostService fosteringPostService;
    private final FosteringSavedPostService fosteringSavedPostService;
    private final UserService userService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.FOSTERING_GET;
    }

    @Override
    public PartialBotApiMethod<?> handle(CallbackQueryContext context) {
        Long userId = context.auth().userInternalId();

        userService.resetFosteringHistoryOffset(userId);

        FosteringRecommendationDTO post = fosteringPostService.findNextUnviewed(userId);

        if (post == null) {
            return noPostsExistMessage(context);
        }

        fosteringPostService.recordView(post.postId(), userId);

        return mapToResponse(context, post);
    }

    private PartialBotApiMethod<?> mapToResponse(CallbackQueryContext context, FosteringRecommendationDTO post) {
        return ResponseBuilder.editPhoto(context.chatId(), context.messageId(), post.petPhotoUrl())
                .caption(formatPostInfo(post))
                .keyboard(buildKeyboard(post, context))
                .build();
    }

    private EditMessageText noPostsExistMessage(CallbackQueryContext context) {
        return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                .text("""
                        📭 Наразі немає доступних оголошень.

                        Спробуйте пізніше!
                        """)
                .keyboard(InlineKeyboardBuilder.builder()
                        .backButtonTo(CallbackId.FOSTERING)
                        .build())
                .build();
    }

    private String formatPostInfo(FosteringRecommendationDTO post) {
        StringBuilder text = new StringBuilder();

        text.append("🐾 ").append(post.petName()).append("\n");

        if (post.petBreed() != null && !post.petBreed().isBlank()) {
            text.append("🏷️ Порода: ").append(post.petBreed()).append("\n");
        }

        if (post.petColor() != null && !post.petColor().isBlank()) {
            text.append("🎨 Колір: ").append(post.petColor()).append("\n");
        }

        text.append("🎂 Вік: ").append(formatAge(post.petAge())).append("\n");

        String sexEmoji = switch (post.petSex()) {
            case MALE -> "♂️";
            case FEMALE -> "♀️";
        };
        String sexText = switch (post.petSex()) {
            case MALE -> "він";
            case FEMALE -> "вона";
        };
        text.append(sexEmoji).append(" Стать: ").append(sexText).append("\n");

        String sizeText = switch (post.petSize()) {
            case SMALL -> "малий";
            case MEDIUM -> "середній";
            case LARGE -> "великий";
        };
        text.append("📏 Розмір: ").append(sizeText).append("\n");

        if (post.petSpecialMarks() != null && !post.petSpecialMarks().isBlank()) {
            text.append("📝 Особливі прикмети: ").append(post.petSpecialMarks()).append("\n");
        }

        if (post.plannedDurationDays() != null) {
            text.append("⏳ Тривалість перетримки: до ").append(post.plannedDurationDays()).append(" днів\n");
        }

        if (post.ownerComment() != null && !post.ownerComment().isBlank()) {
            text.append("\n💬 ").append(post.ownerComment()).append("\n");
        }

        return text.toString();
    }

    private String formatAge(int age) {
        int lastDigit = age % 10;
        int lastTwoDigits = age % 100;
        if (lastDigit == 1 && lastTwoDigits != 11) {
            return age + " рік";
        } else if (lastDigit >= 2 && lastDigit <= 4 && (lastTwoDigits < 12 || lastTwoDigits > 14)) {
            return age + " роки";
        } else {
            return age + " років";
        }
    }

    private InlineKeyboardMarkup buildKeyboard(FosteringRecommendationDTO post, CallbackQueryContext context) {
        Long userId = context.auth().userInternalId();
        boolean isSaved = fosteringSavedPostService.isSaved(post.postId(), userId);

        return InlineKeyboardBuilder.builder()
                .addButton(isSaved ? "💔 Видалити" : "❤️ Зберегти",
                        isSaved ? CallbackId.FOSTERING_UNSAVE_POST : CallbackId.FOSTERING_SAVE_POST,
                        post.postId())
                .addButton("✉️ Відгукнутись", CallbackId.FOSTERING_RESPONSE_CREATE, post.postId())
                .addButton("➡️ Наступна", CallbackId.FOSTERING_GET_NEXT, post.postId())
                .backButtonTo(CallbackId.FOSTERING)
                .build();
    }
}
