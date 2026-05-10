package op.edu.ua.petbed.telegram.callback.handler.fostering;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.fostering.FosteringResponseService;
import op.edu.ua.petbed.fostering.domain.model.FosteringResponseStatus;
import op.edu.ua.petbed.common.dto.FosteringResponseDTO;
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

@NullMarked
@Component
@RequiredArgsConstructor
public class FosteringMyResponseDetailCallbackHandler implements CallbackHandler {

    private final FosteringResponseService fosteringResponseService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
            .ofPattern("dd.MM.yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.FOSTERING_MY_RESPONSE_DETAIL;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        Long responseId = context.callbackData().entityId();
        if (responseId == null) {
            return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                    .text("❌ Помилка: ID відгуку не вказано")
                    .keyboard(InlineKeyboardBuilder.builder()
                            .backButtonTo(CallbackId.FOSTERING_MY_RESPONSES)
                            .build())
                    .build();
        }

        FosteringResponseDTO response = fosteringResponseService.findById(responseId);

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

    private String formatResponseInfo(FosteringResponseDTO response) {
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

    private InlineKeyboardMarkup buildKeyboard(FosteringResponseDTO response, Long responseId) {
        InlineKeyboardBuilder builder = InlineKeyboardBuilder.builder();

        FosteringResponseStatus status = response.status();

        switch (status) {
            case NEW -> builder.addButton(
                    "❌ Відмінити",
                    CallbackId.FOSTERING_MY_RESPONSE_DETAIL_CANCEL,
                    responseId
            );
            case CONFIRMED_BY_OWNER -> {
                builder.addButton(
                        "🏁 Фінальне підтвердження",
                        CallbackId.FOSTERING_MY_RESPONSE_DETAIL_FINAL_CONFIRM,
                        responseId
                );
                builder.addButton(
                        "❌ Відмінити",
                        CallbackId.FOSTERING_MY_RESPONSE_DETAIL_CANCEL,
                        responseId
                );
            }
            case REJECTED_BY_OWNER, FINAL_CONFIRMED, CANCELLED -> {}
        }

        builder.backButtonTo(CallbackId.FOSTERING_MY_RESPONSES);

        return builder.build();
    }
}
