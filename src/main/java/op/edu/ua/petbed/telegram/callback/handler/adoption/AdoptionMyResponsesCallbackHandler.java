package op.edu.ua.petbed.telegram.callback.handler.adoption;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.adoption.AdoptionResponseService;
import op.edu.ua.petbed.common.dto.AdoptionResponseDTO;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.response.CallbackListItem;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.KeyboardLayout;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

/**
 * Handler for ADOPTION_MY_RESPONSES callback - shows list of user's responses with pagination.
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
        int offset = context.callbackData().offset() != null ? context.callbackData().offset() : 0;

        Page<AdoptionResponseDTO> responses = adoptionResponseService.findByResponderId(userId,
                PageRequest.of(offset, KeyboardLayout.DEFAULT.pageSize()));

        if (responses.isEmpty()) {
            return noResponsesMessage(context);
        }

        return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                .text(formatHeader(responses))
                .keyboard(buildKeyboard(context, responses))
                .build();
    }

    private BotApiMethod<?> noResponsesMessage(CallbackQueryContext context) {
        return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                .text("""
                        ✉️ Мої відгуки

                        Ви ще не відгукувались на жодне оголошення.
                        """)
                .keyboard(InlineKeyboardBuilder.builder()
                        .backButtonFor(CallbackId.ADOPTION)
                        .build())
                .build();
    }

    private String formatHeader(Page<AdoptionResponseDTO> responses) {
        return "✉️ Мої відгуки";
    }

    private InlineKeyboardMarkup buildKeyboard(CallbackQueryContext context, Page<AdoptionResponseDTO> responses) {
        return InlineKeyboardBuilder.builder()
                .paginatedList(toPageDto(responses), context.callbackData())
                .backButtonFor(CallbackId.ADOPTION)
                .build();
    }

    private Page<CallbackListItem> toPageDto(Page<AdoptionResponseDTO> responses) {
        return responses.map(response -> new CallbackListItem(
                CallbackId.ADOPTION_RESPONSE_SINGLE,
                response.id(),
                response.getStatusEmoji() + " " + response.postPetName()
        ));
    }
}
