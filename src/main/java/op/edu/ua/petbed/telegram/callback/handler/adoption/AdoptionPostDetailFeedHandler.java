package op.edu.ua.petbed.telegram.callback.handler.adoption;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.adoption.AdoptionPostService;
import op.edu.ua.petbed.adoption.AdoptionSavedPostService;
import op.edu.ua.petbed.adoption.application.dto.AdoptionPostDetailDTO;
import op.edu.ua.petbed.telegram.callback.CallbackData;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;

/**
 * Handler for showing adoption post detail in feed view (for non-owners).
 */
@NullMarked
@Component
@RequiredArgsConstructor
public class AdoptionPostDetailFeedHandler implements CallbackHandler {

    private final AdoptionPostService adoptionPostService;
    private final AdoptionSavedPostService adoptionSavedPostService;

    @Override
    public CallbackId getCallbackId() {
        // Using ADOPTION_POST_DETAIL which is the detail view
        return CallbackId.ADOPTION_POST_DETAIL;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        Long postId = context.callbackData().entityId();
        if (postId == null) {
            return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                    .text("❌ Помилка: ID оголошення не вказано")
                    .keyboard(InlineKeyboardBuilder.builder()
                            .backButtonTo(CallbackId.ADOPTION_GET)
                            .build())
                    .build();
        }

        Long userId = context.auth().userInternalId();
        AdoptionPostDetailDTO detail = adoptionPostService.findById(postId);
        var post = detail.post();
        boolean isSaved = adoptionSavedPostService.isSaved(postId, userId);

        // Build message
        StringBuilder text = new StringBuilder();
        text.append("🐾 ").append(post.petName()).append("\n\n");

        if (post.petBreed() != null && !post.petBreed().isBlank()) {
            text.append("🏷️ Порода: ").append(post.petBreed()).append("\n");
        }
        if (post.petColor() != null && !post.petColor().isBlank()) {
            text.append("🎨 Колір: ").append(post.petColor()).append("\n");
        }
        if (post.ownerComment() != null && !post.ownerComment().isBlank()) {
            text.append("\n💬 ").append(post.ownerComment()).append("\n");
        }

        // Build keyboard
        InlineKeyboardBuilder builder = InlineKeyboardBuilder.builder();
        builder.addButton("✉️ Відгукнутись",
                CallbackData.of(CallbackId.ADOPTION_RESPONSE_CREATE, postId, null));

        if (isSaved) {
            builder.addButton("💔 Видалити зі збережених",
                    CallbackData.of(CallbackId.ADOPTION_UNSAVE_POST, postId, null));
        } else {
            builder.addButton("❤️ Зберегти",
                    CallbackData.of(CallbackId.ADOPTION_SAVE_POST, postId, null));
        }

        builder.backButtonTo(CallbackId.ADOPTION_GET);

        return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                .text(text.toString())
                .keyboard(builder.build())
                .build();
    }
}
