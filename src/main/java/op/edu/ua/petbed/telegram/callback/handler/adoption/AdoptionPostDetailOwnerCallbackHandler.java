package op.edu.ua.petbed.telegram.callback.handler.adoption;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.adoption.AdoptionPostService;
import op.edu.ua.petbed.adoption.AdoptionResponseService;
import op.edu.ua.petbed.adoption.dto.AdoptionPostDetailDTO;
import op.edu.ua.petbed.adoption.dto.AdoptionResponseDTO;
import op.edu.ua.petbed.adoption.domain.model.enums.AdoptionPostStatus;

import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;

import java.util.List;

/**
 * Handler for ADOPTION_POST_DETAIL callback - shows post details with responses for the owner.
 */
@NullMarked
@Component
@RequiredArgsConstructor
public class AdoptionPostDetailOwnerCallbackHandler implements CallbackHandler {

    private final AdoptionPostService adoptionPostService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.ADOPTION_POST_DETAIL;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        Long postId = context.callbackData().entityId();
        if (postId == null) {
            return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                    .text("❌ Помилка: ID оголошення не вказано")
                    .keyboard(InlineKeyboardBuilder.builder()
                            .backButtonFor(CallbackId.ADOPTION_MY_POSTS)
                            .build())
                    .build();
        }

        AdoptionPostDetailDTO detail = adoptionPostService.findById(postId);
        var post = detail.post();
        List<AdoptionResponseDTO> responses = detail.responses();

        // Build message text
        StringBuilder text = new StringBuilder();
        text.append("🐾 ").append(post.petName()).append("\n\n");

        if (post.ownerComment() != null && !post.ownerComment().isBlank()) {
            text.append("💬 ").append(post.ownerComment()).append("\n\n");
        }

        text.append("Статус: ").append(getStatusText(post.status())).append("\n");

        if (!responses.isEmpty()) {
            text.append("\n📨 Відгуків: ").append(responses.size()).append("\n");
        }

        // Build keyboard
        InlineKeyboardBuilder builder = InlineKeyboardBuilder.builder();

        // Add response button if post is active
        if (post.status() == AdoptionPostStatus.ACTIVE && !responses.isEmpty()) {
            builder.addButton("📨 Переглянути відгуки",
                    CallbackId.ADOPTION_RESPONSE_DETAIL, postId);
        }

        // Add cancel button if not completed
        if (post.status() != AdoptionPostStatus.COMPLETED) {
            builder.addButton("🗑️ Скасувати оголошення",
                    CallbackId.ADOPTION_POST_TRY_DELETE, postId);
        }

        builder.backButtonFor(CallbackId.ADOPTION_MY_POSTS);

        return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                .text(text.toString())
                .keyboard(builder.build())
                .build();
    }

    private String getStatusText(AdoptionPostStatus status) {
        return switch (status) {
            case ACTIVE -> "🟢 Активне";
            case PENDING_CONFIRMATION -> "⏳ Очікує підтвердження";
            case COMPLETED -> "✅ Завершено";
            case CANCELLED -> "❌ Скасовано";
        };
    }
}
