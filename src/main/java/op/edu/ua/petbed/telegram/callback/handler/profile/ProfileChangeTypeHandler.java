package op.edu.ua.petbed.telegram.callback.handler.profile;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@NullMarked
@Component
@RequiredArgsConstructor
public class ProfileChangeTypeHandler implements CallbackHandler {

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.PROFILE_CHANGE_TYPE;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        return mapToResponse(context);
    }

    private BotApiMethod<?> mapToResponse(CallbackQueryContext context) {
        String messageText = """
                📋 Зміна типу профілю

                👤 Звичайний — базовий тип користувача
                🌟 Волонтер — додаткові функції:
                • Створення оголошень

                💡 Ви можете змінити тип акаунту.
                """;

        InlineKeyboardMarkup keyboard = InlineKeyboardBuilder.builder()
                .navButtonsFor(CallbackId.PROFILE_CHANGE_TYPE, context.auth().userInternalId())
                .backButtonFor(CallbackId.PROFILE_CHANGE_TYPE)
                .build();

        return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                .text(messageText)
                .keyboard(keyboard)
                .build();
    }
}