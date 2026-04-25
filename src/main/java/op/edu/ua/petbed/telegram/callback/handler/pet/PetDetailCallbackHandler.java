package op.edu.ua.petbed.telegram.callback.handler.pet;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.common.dto.PetDTO;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.common.model.PetStatus;
import op.edu.ua.petbed.pet.PetService;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageMedia;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@NullMarked
@Component
@RequiredArgsConstructor
public class PetDetailCallbackHandler implements CallbackHandler {

    private final PetService petService;
    private final TelegramClient telegramClient;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.PET_DETAIL;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        var entityId = context.callbackData().entityId();
        if (entityId == null) {
            throw new PetBedException("Entity ID is required for PET_DETAIL", PetBedException.ErrorCode.INVALID_CALLBACK);
        }
        PetDTO pet = petService.findById(entityId);


        PartialBotApiMethod<?> photo = ResponseBuilder.telegram()
                .chatId(context.chatId())
                .editMessage(context.messageId())
                .photo(pet.photoId())
                .text(pet.formatInfo())
                .keyboard(actionKeyboard(pet))
                .buildPhoto();
        try {
            if (photo instanceof EditMessageMedia edit) {
                telegramClient.execute(edit);
            } else if (photo instanceof SendPhoto send) {
                telegramClient.execute(send);
            }
        } catch (TelegramApiException e) {
            throw new PetBedException("Failed to send pet photo", PetBedException.ErrorCode.INTERNAL_ERROR);
        }

        return AnswerCallbackQuery.builder()
                .callbackQueryId(context.callbackQuery().getId())
                .build();
    }

    private InlineKeyboardMarkup actionKeyboard(PetDTO pet) {
        return InlineKeyboardBuilder.builder()
                .navButtonsFor(CallbackId.PET_DETAIL, pet.id(), child -> {
                    if (child == CallbackId.PET_UPDATE) {
                        return pet.status() == PetStatus.DEFAULT;
                    }
                    if (child == CallbackId.PET_DELETE) {
                        return pet.status().canDelete();
                    }
                    return false;
                })
                .backButtonFor(CallbackId.PET_DETAIL)
                .build();
    }
}
