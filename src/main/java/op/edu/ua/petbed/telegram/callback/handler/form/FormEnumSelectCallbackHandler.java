package op.edu.ua.petbed.telegram.callback.handler.form;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.form.scheme.FormInput;
import op.edu.ua.petbed.telegram.service.FormService;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import static op.edu.ua.petbed.common.exceptions.PetBedException.ErrorCode.INTERNAL_ERROR;

@Slf4j
@NullMarked
@Component
@RequiredArgsConstructor
public class FormEnumSelectCallbackHandler implements CallbackHandler {

    private final TelegramClient telegramClient;
    private final FormService formService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.FORM_ENUM_SELECT;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        var entity = formService.getActiveFormOrThrow(context.auth().userInternalId());
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

        try {
            telegramClient.execute(EditMessageText.builder()
                    .chatId(entity.getChatId())
                    .messageId(context.messageId())
                    .text(step.prompt() + "\n\n✅ " + selectedValue)
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to edit message", e);
        }

        return formService.processInput(new FormInput.Choice(selectedValue), context.auth().userInternalId(), entity.getChatId());
    }
}