package op.edu.ua.petbed.telegram.callback.handler.lost;

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
import op.edu.ua.petbed.telegram.service.TelegramMessageService;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

/**
 * Handler for LOST_START callback - shows list of user's pets to select for lost search.
 * User can either select an existing pet or add a new one.
 */
@NullMarked
@Component
@RequiredArgsConstructor
public class LostStartCallbackHandler implements CallbackHandler {

    private final PetService petService;
    private final TelegramMessageService telegramMessageService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.LOST_START;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        var callbackData = context.callbackData();
        int offset = callbackData.offset() != null ? callbackData.offset() : 0;

        Page<PetDTO> pets = petService.findAllByOwnerId(
                context.auth().userInternalId(),
                PageRequest.of(offset, KeyboardLayout.DEFAULT.pageSize())
        );

        if (pets.isEmpty()) {
            // If no pets, show message with only "Add Pet" option
            SendMessage message = ResponseBuilder.sendMessage(context.chatId())
                    .text("""
                            🐾 У вас ще немає доданих тварин.
                            
                            Спочатку додайте тварину, щоб запустити пошук.
                            """)
                    .keyboard(InlineKeyboardBuilder.builder()
                            .addButton("➕ Додати тварину", CallbackId.LOST_ADD_PET)
                            .backButtonTo(CallbackId.LOST)
                            .build())
                    .build();
            return telegramMessageService.editOrSend(context, message);
        }

        SendMessage message = ResponseBuilder.sendMessage(context.chatId())
                .text("🔎 Оберіть тварину для пошуку або додайте нову:")
                .keyboard(buildKeyboard(context, pets))
                .build();

        return telegramMessageService.editOrSend(context, message);
    }

    private InlineKeyboardMarkup buildKeyboard(CallbackQueryContext context, Page<PetDTO> pets) {
        return InlineKeyboardBuilder.builder()
                .paginatedList(toPageDto(pets), context.callbackData())
                .addButton("➕ Додати тварину", CallbackId.LOST_ADD_PET)
                .backButtonTo(CallbackId.LOST)
                .build();
    }

    private Page<CallbackListItem> toPageDto(Page<PetDTO> pets) {
        return pets.map(pet -> new CallbackListItem(
                CallbackId.LOST_SELECT_PET,
                pet.id(),
                pet.name()
        ));
    }
}
