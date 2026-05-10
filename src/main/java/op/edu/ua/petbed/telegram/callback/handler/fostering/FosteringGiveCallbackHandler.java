package op.edu.ua.petbed.telegram.callback.handler.fostering;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.common.dto.PetDTO;
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
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

/**
 * Handler for FOSTERING_GIVE callback - shows list of owner's pets that can be put up for fostering.
 * Similar to ADOPTION_GIVE: shows pets list + "Add pet" button.
 */
@NullMarked
@Component
@RequiredArgsConstructor
@Slf4j
public class FosteringGiveCallbackHandler implements CallbackHandler {

    private final PetService petService;
    private final TelegramMessageService telegramMessageService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.FOSTERING_GIVE;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        var callbackData = context.callbackData();
        int offset = callbackData.offset() != null ? callbackData.offset() : 0;

        Page<PetDTO> pets = petService.findPetsAvailableForGive(
                context.auth().userInternalId(),
                PageRequest.of(offset, KeyboardLayout.DEFAULT.pageSize())
        );

        SendMessage message;
        if (pets.isEmpty()) {
            message = ResponseBuilder.sendMessage(context.chatId())
                    .text("""
                            🐾 У вас немає тварин, яких можна запропонувати на перетримку.

                            Спочатку додайте тварину до свого профілю.
                            """)
                    .keyboard(InlineKeyboardBuilder.builder()
                            .addButton("➕ Додати тварину", CallbackId.FOSTERING_ADD_PET)
                            .backButtonTo(CallbackId.FOSTERING)
                            .build())
                    .build();
        } else {
            message = ResponseBuilder.sendMessage(context.chatId())
                    .text("🐾 Оберіть тварину для перетримки:")
                    .keyboard(buildKeyboard(context, pets))
                    .build();
        }

        return telegramMessageService.editOrSend(context, message);
    }

    private InlineKeyboardMarkup buildKeyboard(CallbackQueryContext context, Page<PetDTO> pets) {
        return InlineKeyboardBuilder.builder()
                .paginatedList(toPageDto(pets), context.callbackData())
                .addButton("➕ Додати тварину", CallbackId.FOSTERING_ADD_PET)
                .backButtonTo(CallbackId.FOSTERING)
                .build();
    }

    private Page<CallbackListItem> toPageDto(Page<PetDTO> pets) {
        return pets.map(pet -> new CallbackListItem(
                CallbackId.FOSTERING_SELECT_PET,
                pet.id(),
                pet.name()
        ));
    }
}