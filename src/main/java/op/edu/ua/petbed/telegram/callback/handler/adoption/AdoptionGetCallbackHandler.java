package op.edu.ua.petbed.telegram.callback.handler.adoption;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.adoption.AdoptionPostService;
import op.edu.ua.petbed.adoption.AdoptionSavedPostService;
import op.edu.ua.petbed.adoption.application.dto.AdoptionRecommendationDTO;
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
 * Handler for ADOPTION_GET callback - shows the first post in the adoption feed.
 */
@NullMarked
@Component
@RequiredArgsConstructor
public class AdoptionGetCallbackHandler implements CallbackHandler {

    private final AdoptionPostService adoptionPostService;
    private final AdoptionSavedPostService adoptionSavedPostService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.ADOPTION_GET;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        Long userId = context.auth().userInternalId();

        // Reset offset when starting fresh
        adoptionPostService.resetOffset(userId);

        // Get first post
        AdoptionRecommendationDTO post = adoptionPostService.findNextForFeed(userId, 0);

        if (post == null) {
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

        // Record view
        adoptionPostService.recordView(post.postId(), userId);

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
                CallbackData.of(CallbackId.ADOPTION_RESPONSE_CREATE, post.postId(), null));

        if (post.isSaved()) {
            builder.addButton("💔 Видалити зі збережених",
                    CallbackData.of(CallbackId.ADOPTION_UNSAVE_POST, post.postId(), null));
        } else {
            builder.addButton("❤️ Зберегти",
                    CallbackData.of(CallbackId.ADOPTION_SAVE_POST, post.postId(), null));
        }

        builder.addButton("➡️ Наступна",
                CallbackData.of(CallbackId.ADOPTION_GET_NEXT, null, 1));

        builder.backButtonFor(CallbackId.ADOPTION);

        return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                .text(text.toString())
                .keyboard(builder.build())
                .build();
    }
}
