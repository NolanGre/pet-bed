package op.edu.ua.petbed.telegram.callback.handler.adoption;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.common.dto.PetDTO;
import op.edu.ua.petbed.pet.PetService;
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
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

/**
 * Handler for ADOPTION_GIVE callback - shows list of owner's pets that can be put up for adoption.
 */
@NullMarked
@Component
@RequiredArgsConstructor
public class AdoptionGiveCallbackHandler implements CallbackHandler {

    private final PetService petService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.ADOPTION_GIVE;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        Long userId = context.auth().userInternalId();
        var callbackData = context.callbackData();

        int offset = callbackData.offset() != null ? callbackData.offset() : 0;
        Page<PetDTO> pets = petService.findPetsAvailableForLostSearch(userId,
                PageRequest.of(offset, KeyboardLayout.DEFAULT.pageSize()));

        if (pets.isEmpty()) {
            return petsEmptyMessage(context);
        }

        return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                .text("🐾 Оберіть тварину для передачі:")
                .keyboard(buildKeyboard(context, pets))
                .build();
    }

    private EditMessageText petsEmptyMessage(CallbackQueryContext context) {
        return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                .text("""
                        🐾 У вас немає тварин, яких можна віддати.
                        
                        Спочатку додайте тварину до свого профілю.
                        """)
                .keyboard(InlineKeyboardBuilder.builder()
                        .navButtonsFor(CallbackId.ADOPTION_GIVE)
                        .backButtonFor(CallbackId.ADOPTION)
                        .build())
                .build();
    }

    private InlineKeyboardMarkup buildKeyboard(CallbackQueryContext context, Page<PetDTO> pets) {
        return InlineKeyboardBuilder.builder()
                .paginatedList(toPageDto(pets), context.callbackData())
                .navButtonsFor(getCallbackId())
                .backButtonFor(getCallbackId())
                .build();
    }

    private Page<CallbackListItem> toPageDto(Page<PetDTO> pets) {
        return pets.map(pet -> new CallbackListItem(
                CallbackId.ADOPTION_SELECT_PET,
                pet.id(),
                pet.name()
        ));
    }
}
