package op.edu.ua.petbed.telegram.callback.handler.adoption;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.adoption.AdoptionResponseService;
import op.edu.ua.petbed.common.dto.AdoptionResponseDTO;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.response.CallbackListItem;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.KeyboardLayout;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

/**
 * Handler for ADOPTION_RESPONSES_LIST callback - shows list of responses for a post with pagination.
 */
@NullMarked
@Component
@RequiredArgsConstructor
public class AdoptionResponsesCallbackHandler implements CallbackHandler {

    private final AdoptionResponseService adoptionResponseService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.ADOPTION_RESPONSES_LIST;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        Long postId = context.callbackData().entityId();
        if (postId == null) {
            return errorMessage(context, "ID оголошення не вказано");
        }

        int offset = context.callbackData().offset() != null ? context.callbackData().offset() : 0;
        Page<AdoptionResponseDTO> responses = adoptionResponseService.findByPostId(postId,
                PageRequest.of(offset, KeyboardLayout.DEFAULT.pageSize()));

        if (responses.isEmpty()) {
            return noResponsesMessage(context);
        }

        return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                .text(formatHeader(responses))
                .keyboard(buildKeyboard(context, responses))
                .build();
    }

    private BotApiMethod<?> errorMessage(CallbackQueryContext context, String error) {
        return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                .text("❌ Помилка: " + error)
                .keyboard(InlineKeyboardBuilder.builder()
                        .backButtonTo(CallbackId.ADOPTION_POST_DETAIL)
                        .build())
                .build();
    }

    private BotApiMethod<?> noResponsesMessage(CallbackQueryContext context) {
        return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                .text("📨 Поки немає відгуків на це оголошення.")
                .keyboard(InlineKeyboardBuilder.builder()
                        .backButtonTo(CallbackId.ADOPTION_POST_DETAIL)
                        .build())
                .build();
    }

    private String formatHeader(Page<AdoptionResponseDTO> responses) {
        return "📨 Відгуки на ваше оголошення:";
    }

    private InlineKeyboardMarkup buildKeyboard(CallbackQueryContext context, Page<AdoptionResponseDTO> responses) {
        return InlineKeyboardBuilder.builder()
                .paginatedList(toPageDto(responses), context.callbackData())
                .backButtonTo(CallbackId.ADOPTION_POST_DETAIL)
                .build();
    }

    private Page<CallbackListItem> toPageDto(Page<AdoptionResponseDTO> responses) {
        return responses.map(response -> new CallbackListItem(
                CallbackId.ADOPTION_RESPONSE_SINGLE,
                response.id(),
                response.getStatusEmoji() + " " + truncate(response.responderUsername(), 20)
        ));
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.isBlank()) {
            return "(без імені)";
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }
}
