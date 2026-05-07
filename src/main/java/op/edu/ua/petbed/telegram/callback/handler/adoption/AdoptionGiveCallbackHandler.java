package op.edu.ua.petbed.telegram.callback.handler.adoption;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.common.dto.PetDTO;
import op.edu.ua.petbed.pet.PetService;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.form.scheme.FormType;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import op.edu.ua.petbed.telegram.service.FormService;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;

import java.util.List;

/**
 * Handler for ADOPTION_GIVE callback - shows list of owner's pets that can be put up for adoption.
 */
@NullMarked
@Component
@RequiredArgsConstructor
public class AdoptionGiveCallbackHandler implements CallbackHandler {

    private final PetService petService;
    private final FormService formService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.ADOPTION_GIVE;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        Long userId = context.auth().userInternalId();

        // Get pets that are available for adoption (status DEFAULT)
        List<PetDTO> pets = petService.findPetsAvailableForLostSearch(userId, PageRequest.of(0, 10))
                .getContent();

        if (pets.isEmpty()) {
            // No pets available - suggest adding a new one
            return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                    .text("""
                            🐾 У вас немає тварин, яких можна віддати.
                            
                            Спочатку додайте тварину до свого профілю.
                            """)
                    .keyboard(InlineKeyboardBuilder.builder()
                            .addButton("➕ Додати тварину", CallbackId.ADD_PET)
                            .backButtonFor(CallbackId.ADOPTION)
                            .build())
                    .build();
        }

        // Build keyboard with pet list and "Add new" button
        InlineKeyboardBuilder builder = InlineKeyboardBuilder.builder();

        pets.forEach(pet -> builder.addButton(
                pet.name(),
                CallbackId.ADOPTION_SELECT_PET,
                pet.id()
        ));

        builder.addButton("➕ Додати нову тварину", CallbackId.ADOPTION_ADD_PET);
        builder.backButtonFor(CallbackId.ADOPTION);

        return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                .text("🐾 Оберіть тварину для передачі:")
                .keyboard(builder.build())
                .build();
    }
}
