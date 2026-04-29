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
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Handler for LOST_RECOMMENDATIONS callback - shows the first recommendation for the owner.
 * Displays found pet photo with details and provides navigation buttons.
 */
@Slf4j
@NullMarked
@Component
@RequiredArgsConstructor
public class LostRecommendationsCallbackHandler implements CallbackHandler {

    private final MatchQueueQueryService matchQueueService;
    private final TelegramClient telegramClient;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
            .ofPattern("dd.MM.yyyy")
            .withZone(ZoneId.systemDefault());

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.LOST_RECOMMENDATIONS;
    }

    @Override
    public PartialBotApiMethod<?> handle(CallbackQueryContext context) {
        Long lostRequestId = context.callbackData().entityId();
        if (lostRequestId == null) {
            throw new IllegalArgumentException("Entity ID is required for LOST_RECOMMENDATIONS");
        }

        Optional<MatchRecommendationDTO> recommendationOpt = matchQueueService.getNextRecommendation(lostRequestId);

        if (recommendationOpt.isEmpty()) {
            return noRecommendationsMessage(context, lostRequestId);
        }

        MatchRecommendationDTO recommendation = recommendationOpt.get();

        // Edit current message to show first recommendation
        return ResponseBuilder.editPhoto(context.chatId(), context.messageId(), recommendation.photoUrl())
                .caption(buildCaption(recommendation))
                .keyboard(buildKeyboard(recommendation, lostRequestId))
                .build();
    }

    private String buildCaption(MatchRecommendationDTO dto) {
        StringBuilder sb = new StringBuilder();
        sb.append("🔍 Можлива знахідка\n\n");

        sb.append("📅 Анкета від: ").append(DATE_FORMATTER.format(dto.foundRequestCreatedAt())).append("\n");

        if (dto.distanceKm() != null) {
            sb.append(String.format("📍 Відстань: %.1f км\n", dto.distanceKm()));
        }

        sb.append(String.format("🎯 Схожість: %.1f%%\n\n", dto.score().doubleValue() * 100));

        sb.append("📝 Опис:\n").append(dto.description());

        return sb.toString();
    }

    private InlineKeyboardMarkup buildKeyboard(MatchRecommendationDTO dto, Long lostRequestId) {
        return InlineKeyboardBuilder.builder()
                .addButton("➡️ Наступна", CallbackId.LOST_RECOMMENDATION_NEXT, lostRequestId)
                .addButton("🐾 Це моя тварина", CallbackId.LOST_CLAIM_PET, dto.matchQueueId())
                .backButtonTo(CallbackId.LOST_ACTIVE_DETAIL, lostRequestId)
                .build();
    }

    private BotApiMethod<?> noRecommendationsMessage(CallbackQueryContext context, Long lostRequestId) {
        return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                .text("🔍 Поки немає рекомендацій.\n\nПеревірте пізніше — ми повідомимо вас про можливі збіги.")
                .keyboard(InlineKeyboardBuilder.builder()
                        .backButtonTo(CallbackId.LOST_ACTIVE_DETAIL, lostRequestId)
                        .build())
                .build();
    }
}