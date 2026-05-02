package op.edu.ua.petbed.telegram.callback.handler.lost;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.lost.LostRequestService;
import op.edu.ua.petbed.lost.MatchRecommendationDTO;
import op.edu.ua.petbed.lost.MatchQueueQueryService;
import op.edu.ua.petbed.pet.PetService;
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

/**
 * Handler for LOST_CLAIM_PET_CONFIRM callback - confirms the pet claim.
 * Updates pet location and marks the match as confirmed.
 */
@Slf4j
@NullMarked
@Component
@RequiredArgsConstructor
public class LostClaimPetConfirmCallbackHandler implements CallbackHandler {

    private final MatchQueueQueryService matchQueueService;
    private final LostRequestService lostRequestService;
    private final PetService petService;
    private final TelegramMessageService telegramMessageService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.LOST_CLAIM_PET_CONFIRM;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        Long matchQueueId = context.callbackData().entityId();
        if (matchQueueId == null) {
            throw new IllegalArgumentException("Entity ID is required for LOST_CLAIM_PET_CONFIRM");
        }

        // Get match queue entry
        MatchRecommendationDTO recommendation = matchQueueService.findById(matchQueueId)
                .orElseThrow(() -> new PetBedException("Match queue entry not found",
                        PetBedException.ErrorCode.MATCH_QUEUE_ENTRY_NOT_PERSISTED));

        Long lostRequestId = recommendation.lostRequestId();
        Long petId = lostRequestService.findById(lostRequestId).petId();

        // Mark match as confirmed
        matchQueueService.markAsConfirmed(matchQueueId);

        // Update lost request location to where the pet was found
        var foundLocation = recommendation.location();
        lostRequestService.updateLocation(lostRequestId, foundLocation);

        log.info("User {} confirmed pet claim for matchQueueId={}, lostRequestId={}, petId={}",
                context.auth().userInternalId(), matchQueueId, lostRequestId, petId);

        // Show success message
        SendMessage message = ResponseBuilder.sendMessage(context.chatId())
                .text(buildSuccessMessage())
                .keyboard(InlineKeyboardBuilder.builder()
                        .backButtonFor(CallbackId.LOST_ACTIVE_DETAIL, lostRequestId)
                        .build())
                .build();

        return telegramMessageService.editOrSend(context, message);
    }

    private String buildSuccessMessage() {
        return """
                ✅ Знахідку підтверджено!

                Геолокацію тварини оновлено.
                Тепер ви можете зв'язатися з людиною, що знайшла тварину.

                Не забудьте скасувати пошук, коли заберете тварину.
                """;
    }
}