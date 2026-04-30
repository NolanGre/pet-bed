package op.edu.ua.petbed.telegram.callback.handler.lost;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.lost.MatchRecommendationDTO;
import op.edu.ua.petbed.lost.MatchQueueQueryService;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import op.edu.ua.petbed.telegram.service.TelegramMessageService;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Handler for LOST_CLAIM_PET callback - shows confirmation dialog before claiming a pet.
 * Displays warning that this will update the pet's location.
 */
@Slf4j
@NullMarked
@Component
@RequiredArgsConstructor
public class LostClaimPetCallbackHandler implements CallbackHandler {

    private final MatchQueueQueryService matchQueueService;
    private final TelegramMessageService telegramMessageService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
            .ofPattern("dd.MM.yyyy")
            .withZone(ZoneId.systemDefault());

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.LOST_CLAIM_PET;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        Long matchQueueId = context.callbackData().entityId();
        if (matchQueueId == null) {
            throw new IllegalArgumentException("Entity ID is required for LOST_CLAIM_PET");
        }

        // Get match queue entry to find lost request ID
        MatchRecommendationDTO recommendation = matchQueueService.findById(matchQueueId)
                .orElseThrow(() -> new PetBedException("Match queue entry not found",
                        PetBedException.ErrorCode.MATCH_QUEUE_ENTRY_NOT_PERSISTED));

        Long lostRequestId = recommendation.lostRequestId();

        String messageText = buildConfirmationMessage(recommendation);

        SendMessage message = ResponseBuilder.sendMessage(context.chatId())
                .text(messageText)
                .keyboard(InlineKeyboardBuilder.builder()
                        .addButton("✅ Підтвердити", CallbackId.LOST_CLAIM_PET_CONFIRM, matchQueueId)
                        .backButtonFor(CallbackId.LOST_RECOMMENDATIONS, lostRequestId)
                        .build())
                .build();

        return telegramMessageService.editOrSend(context, message);
    }

    private String buildConfirmationMessage(MatchRecommendationDTO dto) {
        StringBuilder sb = new StringBuilder();
        sb.append("🐾 Підтвердження знахідки\n\n");

        sb.append("Ви впевнені, що це ваша тварина?\n\n");
        sb.append("📅 Анкета від: ").append(DATE_FORMATTER.format(dto.foundRequestCreatedAt())).append("\n");
        sb.append(String.format("📍 Відстань: %.1f км\n", dto.distanceKm()));
        sb.append(String.format("🎯 Схожість: %.1f%%\n\n", dto.score().doubleValue() * 100));

        sb.append("⚠️ Ящо ви зараз повернетесь, тварину більше не можна буде підтвердити.\n");
        sb.append("📝 Опис:\n").append(dto.description()).append("\n\n");
        sb.append("⚠️ При підтвердженні:\n");
        sb.append("• Геолокація тварини буде оновлена\n");
        sb.append("• Пошук залишиться активним");

        return sb.toString();
    }
}