package op.edu.ua.petbed.telegram.form.handler;

import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.form.FormEntity;
import op.edu.ua.petbed.telegram.form.scheme.FormType;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;

@Slf4j
@Component
@NullMarked
public class AddPetHandled implements FormSubmissionHandler {

    @Override
    public FormType getFormType() {
        return FormType.ADD_PET;
    }

    @Override
    public BotApiMethod<?> handle(FormEntity formEntity) {

        // call pet service
        log.debug("Add Pet Handled: {}",  formEntity);

        return ResponseBuilder.telegram()
                .chatId(formEntity.getChatId())
                .text("Тварину успішно додано")
                .keyboard(InlineKeyboardBuilder.builder()
                        .backButtonTo(formEntity.getReturnCallback())
                        .build())
                .build();
    }
}
