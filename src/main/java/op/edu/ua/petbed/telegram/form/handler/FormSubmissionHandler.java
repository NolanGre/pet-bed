package op.edu.ua.petbed.telegram.form.handler;

import op.edu.ua.petbed.telegram.form.FormData;
import op.edu.ua.petbed.telegram.form.scheme.FormType;
import org.jspecify.annotations.NullMarked;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;

@NullMarked
public interface FormSubmissionHandler {
    FormType getFormType();

    BotApiMethod<?> handle(FormData formEntity);
}
