package op.edu.ua.petbed.telegram.callback.handler.feed;

import op.edu.ua.petbed.common.dto.UserDTO;
import op.edu.ua.petbed.common.model.UserType;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.form.scheme.FormType;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import op.edu.ua.petbed.telegram.service.FormService;
import op.edu.ua.petbed.user.UserService;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;

@NullMarked
@Component
public class FeedCreateCallbackHandler implements CallbackHandler {

    private final UserService userService;
    private final FormService formService;

    public FeedCreateCallbackHandler(UserService userService, FormService formService) {
        this.userService = userService;
        this.formService = formService;
    }

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.FEED_CREATE;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        Long userId = context.auth().userInternalId();
        UserDTO user = userService.findById(userId);

        if (user.type() != UserType.VOLUNTEER) {
            return ResponseBuilder.sendMessage(context.chatId())
                    .text("Тільки волонтери можуть створювати публікації")
                    .keyboard(InlineKeyboardBuilder.builder()
                            .backButtonFor(CallbackId.FEED)
                            .build())
                    .build();
        }

        return formService.startCreateForm(
                FormType.CREATE_FEED_POST,
                CallbackId.FEED,
                userId,
                context.chatId()
        );
    }
}