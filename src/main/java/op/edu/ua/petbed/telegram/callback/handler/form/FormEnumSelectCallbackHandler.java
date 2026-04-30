package op.edu.ua.petbed.telegram.callback.handler.form;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.form.scheme.FormInput;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import op.edu.ua.petbed.telegram.service.FormService;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import static op.edu.ua.petbed.common.exceptions.PetBedException.ErrorCode.INTERNAL_ERROR;

@Slf4j
@NullMarked
@Component
@RequiredArgsConstructor
public class FormEnumSelectCallbackHandler implements CallbackHandler {

    private final FormService formService;
    private final TelegramClient telegramClient;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.FORM_ENUM_SELECT;
    }

    @Override
    public PartialBotApiMethod<?> handle(CallbackQueryContext context) {
        Long userId = context.auth().userInternalId();
        var entity = formService.getActiveFormOrThrow(userId);
        var step = entity.nextStep();

        if (!step.isChoice()) {
            throw new PetBedException("Current step is not a choice", INTERNAL_ERROR);
        }

        Long index = context.callbackData().entityId();
        var enumValues = step.enumValues();
        if (enumValues == null || index == null || index < 0 || index >= enumValues.size()) {
            throw new PetBedException("Invalid enum index: " + index, INTERNAL_ERROR);
        }
        String selectedValue = enumValues.get(index.intValue()).name();
        
        // Store the message ID for potential keyboard removal on /cancel or /skip
        formService.updateLastMessageId(userId, context.messageId());
        
        try {
            telegramClient.execute(ResponseBuilder.editMessage(entity.getChatId(), context.messageId())
                    .text(step.prompt() + "\n\n✅ " + selectedValue)
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to edit enum selection message", e);
        }

        // Process input and return the response
        return formService.processInput(new FormInput.Choice(selectedValue), userId, entity.getChatId());
    }
}