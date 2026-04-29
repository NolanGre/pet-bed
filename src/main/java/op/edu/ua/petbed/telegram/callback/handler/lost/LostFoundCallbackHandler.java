package op.edu.ua.petbed.telegram.callback.handler.lost;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.lost.FinderRecommendationService;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.form.scheme.FormType;
import op.edu.ua.petbed.telegram.service.FormService;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;

/**
 * Handler for LOST_FOUND callback - entry point when user clicks "Я знайшов тварину".
 * Starts the CREATE_FOUND_REQUEST form for reporting a found pet.
 * Clears any existing recommendation cache for the user.
 */
@NullMarked
@Component
@RequiredArgsConstructor
public class LostFoundCallbackHandler implements CallbackHandler {

    private final FormService formService;
    private final FinderRecommendationService finderRecommendationService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.LOST_FOUND;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        Long internalUserId = context.auth().userInternalId();
        Long chatId = context.chatId();

        // Clear any existing recommendation cache when starting new found request
        finderRecommendationService.remove(internalUserId);

        return formService.startCreateForm(
                FormType.CREATE_FOUND_REQUEST,
                CallbackId.MENU,
                internalUserId,
                chatId
        );
    }
}
