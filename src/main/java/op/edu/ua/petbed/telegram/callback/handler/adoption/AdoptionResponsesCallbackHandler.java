package op.edu.ua.petbed.telegram.callback.handler.adoption;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.adoption.AdoptionResponseService;
import op.edu.ua.petbed.adoption.dto.AdoptionResponseDTO;

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
 * Handler for ADOPTION_RESPONSE_DETAIL callback - shows list of responses for a post.
 */
@NullMarked
@Component
@RequiredArgsConstructor
public class AdoptionResponsesCallbackHandler implements CallbackHandler {

    private final AdoptionResponseService adoptionResponseService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.ADOPTION_RESPONSE_DETAIL;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        // Get postId from entityId
        Long postId = context.callbackData().entityId();
        if (postId == null) {
            return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                    .text("❌ Помилка: ID оголошення не вказано")
                    .keyboard(InlineKeyboardBuilder.builder()
                            .backButtonTo(CallbackId.ADOPTION_POST_DETAIL)
                            .build())
                    .build();
        }

        List<AdoptionResponseDTO> responses = adoptionResponseService.findByPostId(postId);

        if (responses.isEmpty()) {
            return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                    .text("📨 Поки немає відгуків на це оголошення.")
                    .keyboard(InlineKeyboardBuilder.builder()
                            .backButtonTo(CallbackId.ADOPTION_POST_DETAIL)
                            .build())
                    .build();
        }

        // Build keyboard with response list
        InlineKeyboardBuilder builder = InlineKeyboardBuilder.builder();

        responses.forEach(response -> {
            String label = response.getStatusEmoji() + " " +
                    response.responderUsername() +
                    " - " + truncate(response.comment(), 20);
            builder.addButton(label,
                    CallbackId.ADOPTION_RESPONSE_DETAIL_CONFIRM,
                    response.id());
        });

        builder.backButtonTo(CallbackId.ADOPTION_POST_DETAIL);

        return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                .text("📨 Відгуки на ваше оголошення:")
                .keyboard(builder.build())
                .build();
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.isBlank()) {
            return "(без коментаря)";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
