package op.edu.ua.petbed.telegram.callback.handler.adoption;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.adoption.AdoptionPostService;
import op.edu.ua.petbed.common.dto.AdoptionPostDTO;
import op.edu.ua.petbed.common.dto.AdoptionPostDetailDTO;
import op.edu.ua.petbed.common.dto.AdoptionResponseDTO;
import op.edu.ua.petbed.common.model.AdoptionPostStatus;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

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

        String text = formatPostInfo(post, responses);

        return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                .text(text)
                .keyboard(buildKeyboard(post, responses, postId))
                .build();
    }

    private String formatPostInfo(AdoptionPostDTO post, List<AdoptionResponseDTO> responses) {
        StringBuilder text = new StringBuilder();
        text.append("🐾 ").append(post.petName()).append("\n\n");

        if (post.ownerComment() != null && !post.ownerComment().isBlank()) {
            text.append("💬 ").append(post.ownerComment()).append("\n\n");
        }

        text.append("Статус: ").append(getStatusText(post.status())).append("\n");

        if (!responses.isEmpty()) {
            text.append("\n📨 Відгуків: ").append(responses.size()).append("\n");
        }

        return text.toString();
    }

    private InlineKeyboardMarkup buildKeyboard(AdoptionPostDTO post, List<AdoptionResponseDTO> responses, Long postId) {
        return InlineKeyboardBuilder.builder()
                .navButtonsFor(CallbackId.ADOPTION_POST_DETAIL, postId, child -> {
                    if (child == CallbackId.ADOPTION_RESPONSES_LIST) {
                        return post.status() == AdoptionPostStatus.ACTIVE && !responses.isEmpty();
                    }
                    if (child == CallbackId.ADOPTION_POST_TRY_DELETE) {
                        return post.status() != AdoptionPostStatus.COMPLETED;
                    }
                    return false;
                })
                .backButtonFor(CallbackId.ADOPTION_POST_DETAIL)
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
