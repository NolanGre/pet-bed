package op.edu.ua.petbed.telegram.callback.handler.lost;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * Handler for LOST_RECOMMENDATION_NEXT callback - shows next recommendation as a new message.
 * Removes keyboard from previous message and sends new message with the next recommendation.
 */
@Slf4j
@NullMarked
@Component
@RequiredArgsConstructor
public class LostRecommendationNextCallbackHandler implements CallbackHandler {

    private final MatchQueueQueryService matchQueueService;
    private final TelegramClient telegramClient;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
            .ofPattern("dd.MM.yyyy")
            .withZone(ZoneId.systemDefault());

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.LOST_RECOMMENDATION_NEXT;
    }

    @Override
    public PartialBotApiMethod<?> handle(CallbackQueryContext context) {
        Long lostRequestId = context.callbackData().entityId();
        if (lostRequestId == null) {
            throw new IllegalArgumentException("Entity ID is required for LOST_RECOMMENDATION_NEXT");
        }

        // Remove keyboard from previous message
        removeKeyboardFromPreviousMessage(context);

        Optional<MatchRecommendationDTO> recommendationOpt = matchQueueService.getNextRecommendation(lostRequestId);

        if (recommendationOpt.isEmpty()) {
            return noMoreRecommendationsMessage(context.chatId());
        }

        MatchRecommendationDTO recommendation = recommendationOpt.get();

        // Send new message with next recommendation
        return ResponseBuilder.sendPhoto(context.chatId(), recommendation.photoUrl())
                .caption(buildCaption(recommendation))
                .keyboard(buildKeyboard(recommendation, lostRequestId))
                .build();
    }

    private void removeKeyboardFromPreviousMessage(CallbackQueryContext context) {
        try {
            telegramClient.execute(EditMessageReplyMarkup.builder()
                    .chatId(context.chatId())
                    .messageId(context.messageId())
                    .replyMarkup(InlineKeyboardMarkup.builder().build())
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to remove keyboard from message {}: {}", context.messageId(), e.getMessage(), e);
        }
    }

    private String buildCaption(MatchRecommendationDTO dto) {
        StringBuilder sb = new StringBuilder();
        sb.append("🔍 Можлива знахідка\n\n");

        sb.append("📅 Анкета від: ").append(DATE_FORMATTER.format(dto.foundRequestCreatedAt())).append("\n");

        if (dto.distanceKm() != null) {
            sb.append(String.format("📍 Відстань: %.1f км\n", dto.distanceKm()));
        }

        sb.append(String.format("🎯 Схожість: %.1f%%\n\n", dto.score().doubleValue() * 100));

        sb.append(formatDescription(dto));

        return sb.toString();
    }

    private String formatDescription(MatchRecommendationDTO dto) {
        StringBuilder sb = new StringBuilder();
        sb.append("📝 Опис:\n");

        if (dto.breedText() != null && !dto.breedText().isBlank()) {
            sb.append("🐩 Порода: ").append(dto.breedText()).append("\n");
        }
        if (dto.colorText() != null && !dto.colorText().isBlank()) {
            sb.append("🎨 Колір: ").append(dto.colorText()).append("\n");
        }
        if (dto.coatText() != null && !dto.coatText().isBlank()) {
            sb.append("🖼️ Окрас: ").append(dto.coatText()).append("\n");
        }
        if (dto.sizeText() != null && !dto.sizeText().isBlank()) {
            sb.append("📏 Розмір: ").append(dto.sizeText()).append("\n");
        }
        if (dto.sexText() != null && !dto.sexText().isBlank()) {
            sb.append("⚧ Стать: ").append(dto.sexText()).append("\n");
        }
        if (dto.featuresText() != null && !dto.featuresText().isBlank()) {
            sb.append("✨ Особливі прикмети: ").append(dto.featuresText()).append("\n");
        }

        return sb.toString().trim();
    }

    private InlineKeyboardMarkup buildKeyboard(MatchRecommendationDTO dto, Long lostRequestId) {
        return InlineKeyboardBuilder.builder()
                .addButton("➡️ Наступна", CallbackId.LOST_RECOMMENDATION_NEXT, lostRequestId)
                .addButton("🐾 Це моя тварина", CallbackId.LOST_CLAIM_PET, dto.matchQueueId())
                .backButtonTo(CallbackId.LOST_ACTIVE_DETAIL, lostRequestId)
                .build();
    }

    private BotApiMethod<?> noMoreRecommendationsMessage(Long chatId) {
        return ResponseBuilder.sendMessage(chatId)
                .text("🔍 Ви переглянули всі доступні рекомендації.\n\nМи повідомимо вас про нові збіги.")
                .keyboard(InlineKeyboardBuilder.builder()
                        .backButtonTo(CallbackId.LOST_ACTIVE)
                        .build())
                .build();
    }
}