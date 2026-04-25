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
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageMedia;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@NullMarked
@Component
@RequiredArgsConstructor
public class PetDetailCallbackHandler implements CallbackHandler {

    private final PetService petService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.PET_DETAIL;
    }

    @Override
    public PartialBotApiMethod<?> handle(CallbackQueryContext context) {
        var entityId = context.callbackData().entityId();
        if (entityId == null) {
            throw new PetBedException("Entity ID is required for PET_DETAIL", PetBedException.ErrorCode.INVALID_CALLBACK);
        }
        PetDTO pet = petService.findById(entityId);

        return ResponseBuilder.editPhoto(context.chatId(), context.messageId(), pet.photoId())
                .caption(pet.formatInfo())
                .keyboard(actionKeyboard(pet))
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
