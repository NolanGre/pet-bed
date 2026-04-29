package op.edu.ua.petbed.telegram.callback.handler.lost;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.common.dto.LostRequestDTO;
import op.edu.ua.petbed.common.dto.PetDTO;
import op.edu.ua.petbed.common.model.PetType;
import op.edu.ua.petbed.lost.LostRequestService;
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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Handler for LOST_ACTIVE_DETAIL callback - displays details of a specific active lost search.
 * Shows pet information and provides actions:
 * - View recommendations (potential matches)
 * - Finish search (cancel)
 * - Go back to list
 */
@NullMarked
@Component
@RequiredArgsConstructor
public class LostActiveDetailCallbackHandler implements CallbackHandler {

    private final LostRequestService lostRequestService;
    private final PetService petService;
    private final TelegramMessageService telegramMessageService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
            .ofPattern("dd.MM.yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.LOST_ACTIVE_DETAIL;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        Long lostRequestId = context.callbackData().entityId();
        if (lostRequestId == null) {
            throw new IllegalArgumentException("Entity ID is required for LOST_ACTIVE_DETAIL");
        }

        LostRequestDTO lostRequest = lostRequestService.findById(lostRequestId);
        PetDTO pet = petService.findById(lostRequest.petId());

        String messageText = buildMessageText(lostRequest, pet);

        SendMessage message = ResponseBuilder.sendMessage(context.chatId())
                .text(messageText)
                .keyboard(buildKeyboard(lostRequestId))
                .build();

        return telegramMessageService.editOrSend(context, message);
    }

    private String buildMessageText(LostRequestDTO lostRequest, PetDTO pet) {
        StringBuilder sb = new StringBuilder();
        sb.append("🔍 Активний пошук\n\n");

        sb.append("🐾 ").append(pet.name()).append("\n");
        sb.append("📋 Тип: ").append(formatPetType(pet.type())).append("\n");
        if (pet.breed() != null && !pet.breed().isBlank()) {
            sb.append("🏷️ Порода: ").append(pet.breed()).append("\n");
        }
        if (pet.color() != null && !pet.color().isBlank()) {
            sb.append("🎨 Колір: ").append(pet.color()).append("\n");
        }
        sb.append("\n");

        sb.append("📅 Пошук розпочато: ").append(DATE_FORMATTER.format(lostRequest.createdAt())).append("\n");
        sb.append("📞 Контакт: ").append(lostRequest.contactInfo()).append("\n");

        sb.append("\n💡 Тут ви можете переглядати рекомендації — потенційні знахідки вашої тварини.");

        return sb.toString();
    }

    private InlineKeyboardMarkup buildKeyboard(Long lostRequestId) {
        return InlineKeyboardBuilder.builder()
                .addButton("💡 Рекомендації", CallbackId.LOST_RECOMMENDATIONS, lostRequestId)
                .addButton("🏁 Завершити пошук", CallbackId.LOST_FINISH, lostRequestId)
                .backButtonTo(CallbackId.LOST_ACTIVE)
                .build();
    }

    private String formatPetType(PetType type) {
        return switch (type) {
            case DOG -> "Собака";
            case CAT -> "Кіт";
            case BIRD -> "Птах";
            case REPTILE -> "Рептилія";
            case RABBIT -> "Кролик";
            case HAMSTER -> "Хом'як";
            case GUINEA_PIG -> "Морська свинка";
            case TURTLE -> "Черепаха";
            case OTHER -> "Інше";
        };
    }
}
