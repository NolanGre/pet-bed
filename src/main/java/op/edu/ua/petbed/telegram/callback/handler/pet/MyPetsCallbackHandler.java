package op.edu.ua.petbed.telegram.callback.handler.pet;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.common.dto.PetDTO;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.pet.PetService;
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
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@NullMarked
@Component
@RequiredArgsConstructor
public class MyPetsCallbackHandler implements CallbackHandler {

    private final PetService petService;
    private final TelegramMessageService messageService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.MY_PETS;
    }

    @Override
    public PartialBotApiMethod<?> handle(CallbackQueryContext context) {
        var callbackData = context.callbackData();
        if (callbackData.offset() == null) {
            throw new PetBedException("Offset must be not null: " + callbackData, PetBedException.ErrorCode.INVALID_CALLBACK);
        }
        var result = petService.findAllByOwnerId(context.auth().userInternalId(),
                PageRequest.of(callbackData.offset(), KeyboardLayout.DEFAULT.pageSize()));

        SendMessage message = ResponseBuilder.sendMessage(context.chatId())
                .text("🐾 Мої улюбленці")
                .keyboard(buildKeyboard(context, result))
                .build();

        messageService.editOrReplace(context.messageId(), message);

        return AnswerCallbackQuery.builder()
                .callbackQueryId(context.callbackQuery().getId())
                .build();
    }


    private InlineKeyboardMarkup buildKeyboard(CallbackQueryContext context, Page<PetDTO> result) {
        return InlineKeyboardBuilder.builder()
                .paginatedList(toPageDto(result), context.callbackData())
                .navButtonsFor(getCallbackId())
                .backButtonFor(getCallbackId())
                .build();
    }

    private Page<CallbackListItem> toPageDto(Page<PetDTO> result) {
        return result.map(pet -> new CallbackListItem(
                CallbackId.PET_DETAIL,
                pet.id(),
                pet.name()
        ));
    }
}
