package op.edu.ua.petbed.telegram.form.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.telegram.form.FormData;
import op.edu.ua.petbed.telegram.form.scheme.FormStep;
import op.edu.ua.petbed.telegram.form.scheme.FormType;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import op.edu.ua.petbed.user.UserService;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;

@Slf4j
@Component
@NullMarked
@RequiredArgsConstructor
public class SetGeolocationHandler implements FormSubmissionHandler {

    private final UserService userService;

    @Override
    public FormType getFormType() {
        return FormType.SET_GEOLOCATION;
    }

    @Override
    public BotApiMethod<?> handle(FormData data) {
        log.debug("Set Geolocation Handled: {}", data);

        var step = FormType.SET_GEOLOCATION.steps().get(0);
        var location = data.location(step);

        userService.setLocation(data.userId(), location.latitude(), location.longitude());

        return ResponseBuilder.sendMessage(data.chatId())
                .text("✅ Вашу геолокацію збережено!")
                .keyboard(InlineKeyboardBuilder.builder()
                        .backButtonTo(data.returnCallback())
                        .build())
                .build();
    }
}