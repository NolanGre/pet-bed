package op.edu.ua.petbed.telegram.callback.handler.adoption;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.adoption.AdoptionPostService;
import op.edu.ua.petbed.common.dto.AdoptionRecommendationDTO;
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
 * Handler for ADOPTION_GET_NEXT callback - shows next adoption post.
 * Resets user offset to 0 and gets next unviewed post.
 */
@NullMarked
@Component
@RequiredArgsConstructor
public class AdoptionGetNextCallbackHandler implements CallbackHandler {

    private final AdoptionPostService adoptionPostService;
    private final UserService userService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.ADOPTION_GET_NEXT;
    }

    @Override
    public PartialBotApiMethod<?> handle(CallbackQueryContext context) {
        Long userId = context.auth().userInternalId();

        userService.resetAdoptionHistoryOffset(userId);

        AdoptionRecommendationDTO post = adoptionPostService.findNextUnviewed(userId);

        if (post == null) {
            return noPostsExistMessage(context);
        }

        adoptionPostService.recordView(post.postId(), userId);

        return mapToResponse(context, post);
    }

    private EditMessageText mapToResponse(CallbackQueryContext context, AdoptionRecommendationDTO post) {
        return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                .text(formatPostInfo(post))
                .keyboard(buildKeyboard(post))
                .build();
    }

    private EditMessageText noPostsExistMessage(CallbackQueryContext context) {
        return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                .text("""
                    📭 Наразі немає доступних анкет.

                    Спробуйте пізніше!
                    """)
                .keyboard(InlineKeyboardBuilder.builder()
                        .backButtonTo(CallbackId.ADOPTION)
                        .build())
                .build();
    }

    private String formatPostInfo(AdoptionRecommendationDTO post) {
        StringBuilder text = new StringBuilder();

        text.append("🐾 ").append(post.petName()).append("\n");

        if (!post.petBreed().isBlank()) {
            text.append("🏷️ Порода: ").append(post.petBreed()).append("\n");
        }

        if (!post.petColor().isBlank()) {
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

    private InlineKeyboardMarkup buildKeyboard(AdoptionRecommendationDTO post) {
        return InlineKeyboardBuilder.builder()
                .addButton("❤️ Зберегти", CallbackId.ADOPTION_SAVE_POST, post.postId())
                .addButton("✉️ Відгукнутись", CallbackId.ADOPTION_RESPONSE_CREATE, post.postId())
                .addButton("◀️ Минула", CallbackId.ADOPTION_GET_PREV, post.postId())
                .addButton("➡️ Наступна", CallbackId.ADOPTION_GET_NEXT, post.postId())
                .backButtonTo(CallbackId.ADOPTION)
                .build();
    }
}