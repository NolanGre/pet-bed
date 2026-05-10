package op.edu.ua.petbed.telegram.callback.handler.fostering;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.fostering.FosteringResponseService;
import op.edu.ua.petbed.common.dto.FosteringResponseDTO;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.response.CallbackListItem;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.KeyboardLayout;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import op.edu.ua.petbed.telegram.service.TelegramMessageService;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@NullMarked
@Component
@RequiredArgsConstructor
public class FosteringMyResponsesCallbackHandler implements CallbackHandler {

    private final FosteringResponseService fosteringResponseService;
    private final TelegramMessageService messageService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.FOSTERING_MY_RESPONSES;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        Long userId = context.auth().userInternalId();
        int offset = context.callbackData().offset() != null ? context.callbackData().offset() : 0;

        Page<FosteringResponseDTO> responses = fosteringResponseService.findByResponderId(userId,
                PageRequest.of(offset, KeyboardLayout.DEFAULT.pageSize()));

        if (responses.isEmpty()) {
            return noResponsesMessage(context);
        }

        SendMessage message = ResponseBuilder.sendMessage(context.chatId())
                .text(formatHeader(responses))
                .keyboard(buildKeyboard(context, responses))
                .build();

        return messageService.editOrReplace(context, message);
    }

    private SendMessage noResponsesMessage(CallbackQueryContext context) {
        return ResponseBuilder.sendMessage(context.chatId())
                .text("""
                        ✉️ Мої відгуки на перетримку

                        Ви ще не відгукувались на жодне оголошення.
                        """)
                .keyboard(InlineKeyboardBuilder.builder()
                        .backButtonTo(CallbackId.FOSTERING)
                        .build())
                .build();
    }

    private String formatHeader(Page<FosteringResponseDTO> responses) {
        return "✉️ Мої відгуки на перетримку";
    }

    private InlineKeyboardMarkup buildKeyboard(CallbackQueryContext context, Page<FosteringResponseDTO> responses) {
        return InlineKeyboardBuilder.builder()
                .paginatedList(toPageDto(responses), context.callbackData())
                .backButtonTo(CallbackId.FOSTERING)
                .build();
    }

    private Page<CallbackListItem> toPageDto(Page<FosteringResponseDTO> responses) {
        return responses.map(response -> new CallbackListItem(
                CallbackId.FOSTERING_MY_RESPONSE_DETAIL,
                response.id(),
                response.getStatusEmoji() + " " + response.postPetName()
        ));
    }
}
