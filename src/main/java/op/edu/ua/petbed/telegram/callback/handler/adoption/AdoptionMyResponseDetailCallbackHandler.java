package op.edu.ua.petbed.telegram.callback.handler.adoption;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.adoption.AdoptionResponseService;
import op.edu.ua.petbed.adoption.domain.model.AdoptionResponseStatus;
import op.edu.ua.petbed.common.dto.AdoptionResponseDTO;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Handler for ADOPTION_MY_RESPONSE_DETAIL callback - shows response details for the responder.
 * This handler is for the user who submitted the response (not the post owner).
 */
@NullMarked
@Component
@RequiredArgsConstructor
public class AdoptionMyResponseDetailCallbackHandler implements CallbackHandler {

    private final AdoptionResponseService adoptionResponseService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
            .ofPattern("dd.MM.yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.ADOPTION_MY_RESPONSE_DETAIL;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        Long responseId = context.callbackData().entityId();
        if (responseId == null) {
            return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                    .text("❌ Помилка: ID відгуку не вказано")
                    .keyboard(InlineKeyboardBuilder.builder()
                            .backButtonTo(CallbackId.ADOPTION_MY_RESPONSES)
                            .build())
                    .build();
        }

        AdoptionResponseDTO response = adoptionResponseService.findById(responseId);

        // Authorization check: verify that the current user is the responder
        Long currentUserId = context.auth().userInternalId();
        if (!response.responderId().equals(currentUserId)) {
            throw new PetBedException(
                    "Ви не маєте доступу до цього відгуку",
                    PetBedException.ErrorCode.ADOPTION_RESPONSE_NOT_AUTHORIZED
            );
        }

        String text = formatResponseInfo(response);
        InlineKeyboardMarkup keyboard = buildKeyboard(response, responseId);

        return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                .text(text)
                .keyboard(keyboard)
                .build();
    }

    private String formatResponseInfo(AdoptionResponseDTO response) {
        StringBuilder text = new StringBuilder();
        text.append("📋 Оголошення: ").append(response.postPetName()).append("\n\n");
        text.append("Статус: ").append(response.getStatusEmoji()).append("\n");

        if (response.comment() != null && !response.comment().isBlank()) {
            text.append("\n💬 Коментар: ").append(response.comment()).append("\n");
        }

        if (response.createdAt() != null) {
            text.append("\n📅 Створено: ").append(DATE_FORMATTER.format(response.createdAt()));
        }

        return text.toString();
    }

    private InlineKeyboardMarkup buildKeyboard(AdoptionResponseDTO response, Long responseId) {
        InlineKeyboardBuilder builder = InlineKeyboardBuilder.builder();

        AdoptionResponseStatus status = response.status();

        // Add action buttons based on status
        switch (status) {
            case NEW -> builder.addButton(
                    "❌ Відмінити",
                    CallbackId.ADOPTION_MY_RESPONSE_DETAIL_CANCEL,
                    responseId
            );
            case CONFIRMED_BY_OWNER -> {
                builder.addButton(
                        "🏁 Фінальне підтвердження",
                        CallbackId.ADOPTION_MY_RESPONSE_DETAIL_FINAL_CONFIRM,
                        responseId
                );
                builder.addButton(
                        "❌ Відмінити",
                        CallbackId.ADOPTION_MY_RESPONSE_DETAIL_CANCEL,
                        responseId
                );
            }
            case REJECTED_BY_OWNER, FINAL_CONFIRMED, CANCELLED -> {
                // Only back button - no additional actions
            }
        }

        builder.backButtonTo(CallbackId.ADOPTION_MY_RESPONSES);

        return builder.build();
    }
}