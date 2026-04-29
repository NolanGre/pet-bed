package op.edu.ua.petbed.telegram.callback.handler.lost;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.common.dto.LostRequestDTO;
import op.edu.ua.petbed.common.dto.PetDTO;
import op.edu.ua.petbed.lost.FinderRecommendationService;
import op.edu.ua.petbed.lost.LostRequestService;
import op.edu.ua.petbed.pet.PetService;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import op.edu.ua.petbed.telegram.service.TelegramMessageService;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.Optional;

/**
 * Handler for LOST_FOUND_MATCHES callback - shows recommendations to the finder.
 * Displays lost pet information with owner contact details.
 * Each recommendation is shown as a new message with keyboard removed from previous.
 */
@Slf4j
@NullMarked
@Component
@RequiredArgsConstructor
public class LostFoundMatchesCallbackHandler implements CallbackHandler {

    private final FinderRecommendationService finderRecommendationService;
    private final LostRequestService lostRequestService;
    private final PetService petService;
    private final TelegramMessageService telegramMessageService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.LOST_FOUND_MATCHES;
    }

    @Override
    public PartialBotApiMethod<?> handle(CallbackQueryContext context) {
        Long finderId = context.auth().userInternalId();
        Long chatId = context.chatId();
        Integer messageId = context.messageId();

        // Remove keyboard from previous message
        telegramMessageService.removeKeyboard(chatId, messageId);

        // Find next valid recommendation iteratively (avoid recursion to prevent StackOverflowError)
        LostRequestDTO lostRequest = findNextValidRecommendation(finderId);

        if (lostRequest == null) {
            // No more recommendations - clear cache and show message
            finderRecommendationService.remove(finderId);
            return noMoreRecommendationsMessage(chatId);
        }

        // Get pet details
        PetDTO pet = petService.findById(lostRequest.petId());

        // Send new message with recommendation
        return ResponseBuilder.sendPhoto(chatId, pet.photoId())
                .caption(buildCaption(lostRequest, pet))
                .keyboard(buildKeyboard(finderId))
                .build();
    }

    /**
     * Finds the next valid recommendation that has contact info.
     * Uses iterative approach to avoid StackOverflowError.
     */
    private @Nullable LostRequestDTO findNextValidRecommendation(Long finderId) {
        final int MAX_ATTEMPTS = 50; // Prevent infinite loop
        int attempts = 0;

        while (attempts < MAX_ATTEMPTS) {
            Optional<Long> nextLostRequestId = finderRecommendationService.pollNext(finderId);

            if (nextLostRequestId.isEmpty()) {
                return null; // No more recommendations
            }

            LostRequestDTO lostRequest = lostRequestService.findById(nextLostRequestId.get());

            // Only return if contact_info is present
            if (lostRequest.contactInfo() != null && !lostRequest.contactInfo().isBlank()) {
                return lostRequest;
            }

            // Skip this one and continue to next
            attempts++;
        }

        log.warn("Reached max attempts ({}) while searching for valid recommendation for finder {}", MAX_ATTEMPTS, finderId);
        return null;
    }

    private String buildCaption(LostRequestDTO lostRequest, PetDTO pet) {
        StringBuilder sb = new StringBuilder();
        sb.append("🐾 ").append(pet.name()).append("\n\n");

        // Pet details
        if (pet.breed() != null && !pet.breed().isBlank()) {
            sb.append("Порода: ").append(pet.breed()).append("\n");
        }
        if (pet.color() != null && !pet.color().isBlank()) {
            sb.append("Колір: ").append(pet.color()).append("\n");
        }
        if (pet.colorPattern() != null && !pet.colorPattern().isBlank()) {
            sb.append("Окрас: ").append(pet.colorPattern()).append("\n");
        }
        if (pet.sex() != null) {
            String sexText = switch (pet.sex()) {
                case MALE -> "Хлопчик";
                case FEMALE -> "Дівчинка";
            };
            sb.append("Стать: ").append(sexText).append("\n");
        }
        if (pet.size() != null) {
            String sizeText = switch (pet.size()) {
                case SMALL -> "Малий";
                case MEDIUM -> "Середній";
                case LARGE -> "Великий";
            };
            sb.append("Розмір: ").append(sizeText).append("\n");
        }
        if (pet.specialMarks() != null && !pet.specialMarks().isBlank()) {
            sb.append("Особливі прикмети: ").append(pet.specialMarks()).append("\n");
        }

        sb.append("\n📞 Контакт власника: ").append(lostRequest.contactInfo());

        return sb.toString();
    }

    private InlineKeyboardMarkup buildKeyboard(Long finderId) {
        return InlineKeyboardBuilder.builder()
                .addButton("➡️ Наступна", CallbackId.LOST_FOUND_MATCHES)
                .addButton("⬅️ Повернутись", CallbackId.LOST_FOUND)
                .build();
    }

    /**
     * Clears the cache for a finder. Called when user exits the recommendations flow.
     */
    public void clearCache(Long finderId) {
        finderRecommendationService.remove(finderId);
    }

    private BotApiMethod<?> noMoreRecommendationsMessage(Long chatId) {
        return ResponseBuilder.sendMessage(chatId)
                .text("🔍 Ви переглянули всі доступні анкети.\n\nДякуємо за вашу допомогу!")
                .keyboard(InlineKeyboardBuilder.builder()
                        .backButtonTo(CallbackId.MENU)
                        .build())
                .build();
    }
}
