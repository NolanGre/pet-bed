package op.edu.ua.petbed.telegram.callback.handler.profile;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.common.dto.UserDTO;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.common.model.UserType;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import op.edu.ua.petbed.user.UserService;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@NullMarked
@Component
@RequiredArgsConstructor
public class ProfileChangeTypeConfirmHandler implements CallbackHandler {

    private final UserService userService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.PROFILE_CHANGE_TYPE_CONFIRM;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        Long internalId = context.callbackData().entityId();
        if (internalId == null) {
            throw new PetBedException("EntityId is required for PROFILE_CHANGE_TYPE_CONFIRM", PetBedException.ErrorCode.INVALID_CALLBACK);
        }

        UserDTO user = userService.toggleUserType(internalId);
        return mapToResponse(context, user.type());
    }

    private BotApiMethod<?> mapToResponse(CallbackQueryContext context, UserType newType) {
        String typeText = switch (newType) {
            case REGULAR -> "👤 Звичайний";
            case VOLUNTEER -> "🌟 Волонтер";
        };

        String messageText = """
                ✅ Тип акаунту змінено на %s
                """.formatted(typeText);

        InlineKeyboardMarkup keyboard = InlineKeyboardBuilder.builder()
                .backButtonTo(CallbackId.PROFILE)
                .build();

        return ResponseBuilder.telegram()
                .chatId(context.chatId())
                .text(messageText)
                .keyboard(keyboard)
                .editMessage(context.messageId())
                .build();
    }
}