package op.edu.ua.petbed.telegram.callback.handler.fostering;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.fostering.FosteringPostService;
import op.edu.ua.petbed.fostering.FosteringSavedPostService;
import op.edu.ua.petbed.common.dto.FosteringRecommendationDTO;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@NullMarked
@Component
@RequiredArgsConstructor
public class FosteringSavedDetailCallbackHandler implements CallbackHandler {

    private final FosteringPostService fosteringPostService;
    private final FosteringSavedPostService fosteringSavedPostService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.FOSTERING_SAVED_DETAIL;
    }

    @Override
    public PartialBotApiMethod<?> handle(CallbackQueryContext context) {
        Long userId = context.auth().userInternalId();
        Long postId = context.callbackData().entityId();

        if (postId == null) {
            throw new IllegalStateException("Post ID is required for FOSTERING_SAVED_DETAIL");
        }

        FosteringRecommendationDTO post = fosteringPostService.findByIdAsRecommendation(postId);
        if (post == null) {
            return noPostFoundMessage(context);
        }

        return mapToResponse(context, post, userId);
    }

    private PartialBotApiMethod<?> mapToResponse(CallbackQueryContext context,
                                                  FosteringRecommendationDTO post,
                                                  Long userId) {
        boolean isSaved = fosteringSavedPostService.isSaved(post.postId(), userId);

        return ResponseBuilder.editPhoto(context.chatId(), context.messageId(), post.petPhotoUrl())
                .caption(formatPostInfo(post))
                .keyboard(buildKeyboard(post, isSaved))
                .build();
    }

    private EditMessageText noPostFoundMessage(CallbackQueryContext context) {
        return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                .text("❌ Оголошення не знайдено")
                .keyboard(InlineKeyboardBuilder.builder()
                        .backButtonTo(CallbackId.FOSTERING_MY_SAVED)
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

        if (post.petSex() != null) {
            String sexEmoji = switch (post.petSex()) {
                case MALE -> "♂️";
                case FEMALE -> "♀️";
            };
            String sexText = switch (post.petSex()) {
                case MALE -> "він";
                case FEMALE -> "вона";
            };
            text.append(sexEmoji).append(" Стать: ").append(sexText).append("\n");
        }

        if (post.petSize() != null) {
            String sizeText = switch (post.petSize()) {
                case SMALL -> "малий";
                case MEDIUM -> "середній";
                case LARGE -> "великий";
            };
            text.append("📏 Розмір: ").append(sizeText).append("\n");
        }

        if (post.plannedDurationDays() != null) {
            text.append("⏳ Тривалість перетримки: ").append(post.plannedDurationDays()).append(" днів\n");
        }

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

    private InlineKeyboardMarkup buildKeyboard(FosteringRecommendationDTO post, boolean isSaved) {
        var builder = InlineKeyboardBuilder.builder();

        if (isSaved) {
            builder.addButton("💔 Видалити зі збережених", CallbackId.FOSTERING_UNSAVE_POST, post.postId());
        } else {
            builder.addButton("❤️ Зберегти", CallbackId.FOSTERING_SAVE_POST, post.postId());
        }

        builder.addButton("✉️ Відгукнутись", CallbackId.FOSTERING_RESPONSE_CREATE, post.postId());
        builder.backButtonTo(CallbackId.FOSTERING_MY_SAVED);

        return builder.build();
    }
}