package op.edu.ua.petbed.telegram.callback.handler.adoption;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.adoption.AdoptionResponseService;
import op.edu.ua.petbed.adoption.dto.AdoptionResponseDTO;
import op.edu.ua.petbed.telegram.callback.CallbackData;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;

import java.util.List;

/**
 * Handler for ADOPTION_MY_RESPONSES callback - shows list of user's responses.
 */
@NullMarked
@Component
@RequiredArgsConstructor
public class AdoptionMyResponsesCallbackHandler implements CallbackHandler {

    private final AdoptionResponseService adoptionResponseService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.ADOPTION_MY_RESPONSES;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        Long userId = context.auth().userInternalId();

        // Get all responses by this user
        // Note: We need to add findByResponderId to AdoptionResponseService
        // For now, showing placeholder

        return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                .text("""
                        ✉️ Мої відгуки
                        
                        Тут будуть показані ваші відгуки на оголошення про передачу тварин.
                        """)
                .keyboard(InlineKeyboardBuilder.builder()
                        .backButtonFor(CallbackId.ADOPTION)
                        .build())
                .build();
    }
}
