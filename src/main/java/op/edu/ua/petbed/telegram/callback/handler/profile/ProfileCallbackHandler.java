package op.edu.ua.petbed.telegram.callback.handler.profile;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.common.model.UserType;
import op.edu.ua.petbed.telegram.auth.TelegramAuthService;
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
public class ProfileCallbackHandler implements CallbackHandler {

    private final TelegramAuthService authService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.PROFILE;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        return mapToResponse(context);
    }

    private BotApiMethod<?> mapToResponse(CallbackQueryContext context) {
        var auth = context.auth();
        String messageText = String.format("""
                👤 Ваш профіль

                🆔 ID: %d
                👤 Юзернейм: %s
                📋 Тип акаунту: %s

                %s
                """,
                auth.userTelegramId(),
                auth.username(),
                formatUserType(auth.userType()),
                getTypeDescription(auth.userType()));

        return ResponseBuilder.telegram()
                .chatId(context.chatId())
                .text(messageText)
                .keyboard(profileKeyboard())
                .editMessage(context.messageId())
                .build();
    }

    private InlineKeyboardMarkup profileKeyboard() {
        return InlineKeyboardBuilder.builder()
                .navButtonsFor(CallbackId.PROFILE)
                .backButtonFor(CallbackId.PROFILE)
                .build();
    }

    private String formatUserType(UserType type) {
        return type == UserType.VOLUNTEER
                ? "Волонтер"
                : "Звичайний";
    }

    private String getTypeDescription(UserType type) {
        if (type == UserType.VOLUNTEER) {
            return "🌟 Ви волонтер! Маєте доступ до створення оголошень.";
        }
        return "💡 Змінивши тип на волонтера, ви отримаєте доступ до створення оголошень.";
    }
}