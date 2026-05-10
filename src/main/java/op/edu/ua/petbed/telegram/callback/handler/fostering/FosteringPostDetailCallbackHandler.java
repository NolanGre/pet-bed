package op.edu.ua.petbed.telegram.callback.handler.fostering;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.fostering.FosteringPostService;
import op.edu.ua.petbed.fostering.FosteringResponseService;
import op.edu.ua.petbed.common.dto.FosteringPostDetailDTO;
import op.edu.ua.petbed.common.dto.FosteringResponseDTO;
import op.edu.ua.petbed.fostering.domain.model.FosteringPostStatus;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.response.CallbackListItem;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.KeyboardLayout;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import op.edu.ua.petbed.telegram.service.TelegramMessageService;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

/**
 * Handler for FOSTERING_POST_DETAIL callback - shows post details with responses.
 */
@NullMarked
@Component
@RequiredArgsConstructor
@Slf4j
public class FosteringPostDetailCallbackHandler implements CallbackHandler {

    private final FosteringPostService fosteringPostService;
    private final FosteringResponseService fosteringResponseService;
    private final TelegramMessageService messageService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.FOSTERING_POST_DETAIL;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        Long postId = context.callbackData().entityId();
        if (postId == null) {
            return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                    .text("❌ Помилка: ID оголошення не вказано")
                    .keyboard(InlineKeyboardBuilder.builder()
                            .backButtonFor(CallbackId.FOSTERING_MY_POSTS)
                            .build())
                    .build();
        }

        FosteringPostDetailDTO postDetail = fosteringPostService.findById(postId);

        int offset = context.callbackData().offset() != null ? context.callbackData().offset() : 0;
        Page<FosteringResponseDTO> responses = fosteringResponseService.findByPostId(postId,
                PageRequest.of(offset, KeyboardLayout.DEFAULT.pageSize()));

        if (responses.isEmpty()) {
            return messageService.editOrReplace(context, noResponsesMessage(context, postId, postDetail));
        }

        SendMessage message = ResponseBuilder.sendMessage(context.chatId())
                .text(formatHeader(postDetail, responses))
                .keyboard(buildKeyboard(context, responses, postId))
                .build();

        return messageService.editOrReplace(context, message);
    }

    private SendMessage noResponsesMessage(CallbackQueryContext context, Long postId, FosteringPostDetailDTO postDetail) {
        return ResponseBuilder.sendMessage(context.chatId())
                .text(formatPostInfo(postDetail) + "\n\n📨 Поки немає відгуків на це оголошення.")
                .keyboard(InlineKeyboardBuilder.builder()
                        .navButtonsFor(CallbackId.FOSTERING_POST_DETAIL, postId, child ->
                                child == CallbackId.FOSTERING_POST_TRY_DELETE)
                        .backButtonTo(CallbackId.FOSTERING_MY_POSTS)
                        .build())
                .build();
    }

    private String formatHeader(FosteringPostDetailDTO postDetail, Page<FosteringResponseDTO> responses) {
        return formatPostInfo(postDetail) + "\n\n📨 Відгуки на ваше оголошення:";
    }

    private String formatPostInfo(FosteringPostDetailDTO postDetail) {
        var post = postDetail.post();
        StringBuilder text = new StringBuilder();
        text.append("🐾 ").append(post.petName()).append("\n");
        if (post.plannedDurationDays() != null) {
            text.append("⏳ Тривалість: ").append(post.plannedDurationDays()).append(" днів\n");
        }
        text.append("Статус: ").append(formatStatus(post.status())).append("\n");
        if (post.ownerComment() != null && !post.ownerComment().isBlank()) {
            text.append("\n💬 ").append(post.ownerComment()).append("\n");
        }
        return text.toString();
    }

    private String formatStatus(FosteringPostStatus status) {
        return switch (status) {
            case ACTIVE -> "🟢 Активне";
            case PENDING_CONFIRMATION -> "⏳ Очікує підтвердження";
            case COMPLETED -> "✅ Завершено";
            case CANCELLED -> "🚫 Скасовано";
        };
    }

    private InlineKeyboardMarkup buildKeyboard(CallbackQueryContext context, Page<FosteringResponseDTO> responses, Long postId) {
        return InlineKeyboardBuilder.builder()
                .paginatedList(toPageDto(responses), context.callbackData())
                .navButtonsFor(CallbackId.FOSTERING_POST_DETAIL, postId, child ->
                        child == CallbackId.FOSTERING_POST_TRY_DELETE)
                .backButtonTo(CallbackId.FOSTERING_MY_POSTS)
                .build();
    }

    private Page<CallbackListItem> toPageDto(Page<FosteringResponseDTO> responses) {
        return responses.map(response -> new CallbackListItem(
                CallbackId.FOSTERING_RESPONSE_DETAIL,
                response.id(),
                response.getStatusEmoji() + " " + truncate(response.responderTelegramUsername(), 20)
        ));
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.isBlank()) {
            return "(без імені)";
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }
}
