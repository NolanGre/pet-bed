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
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@NullMarked
@Component
@RequiredArgsConstructor
public class MyPetsCallbackHandler implements CallbackHandler {

    private final PetService petService;
    private final TelegramClient telegramClient;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.MY_PETS;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        var callbackData = context.callbackData();
        if (callbackData.offset() == null) {
            throw new PetBedException("Offset must be not null: " + callbackData, PetBedException.ErrorCode.INVALID_CALLBACK);
        }
        var result = petService.findAllByOwnerId(context.auth().userInternalId(),
                PageRequest.of(callbackData.offset(), KeyboardLayout.DEFAULT.pageSize()));

        InlineKeyboardMarkup keyboard = InlineKeyboardBuilder.builder()
                .paginatedList(toPageDto(result), context.callbackData())
                .navButtonsFor(getCallbackId())
                .backButtonFor(getCallbackId())
                .build();

        //TODO refactor
        try {
            // Спробуємо відредагувати текстове повідомлення
            telegramClient.execute(EditMessageText.builder()
                    .chatId(context.chatId())
                    .messageId(context.messageId())
                    .text("🐾 Мої улюбленці")
                    .replyMarkup(keyboard)
                    .build());
        } catch (TelegramApiException e) {
            // Telegram повернув помилку — скоріше за все це медіа-повідомлення
            try {
                telegramClient.execute(DeleteMessage.builder()
                        .chatId(context.chatId())
                        .messageId(context.messageId())
                        .build());
                telegramClient.execute(SendMessage.builder()
                        .chatId(context.chatId().toString())
                        .text("🐾 Мої улюбленці")
                        .replyMarkup(keyboard)
                        .build());
            } catch (TelegramApiException ex) {
                throw new PetBedException("Failed to send pets list", PetBedException.ErrorCode.INTERNAL_ERROR);
            }
        }

        return AnswerCallbackQuery.builder()
                .callbackQueryId(context.callbackQuery().getId())
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
