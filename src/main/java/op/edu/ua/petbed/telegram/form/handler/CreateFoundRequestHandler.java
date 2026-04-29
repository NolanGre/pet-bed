package op.edu.ua.petbed.telegram.form.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.common.model.PetSex;
import op.edu.ua.petbed.common.model.PetSize;
import op.edu.ua.petbed.common.model.PetType;
import op.edu.ua.petbed.lost.FoundRequestService;
import op.edu.ua.petbed.telegram.callback.CallbackData;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.form.FormData;
import op.edu.ua.petbed.telegram.form.scheme.FormStep;
import op.edu.ua.petbed.telegram.form.scheme.FormType;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import op.edu.ua.petbed.user.UserService;
import org.jspecify.annotations.NullMarked;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.List;

@Slf4j
@Component
@NullMarked
@RequiredArgsConstructor
public class CreateFoundRequestHandler implements FormSubmissionHandler {

    private final FoundRequestService foundRequestService;
    private final UserService userService;

    @Override
    public FormType getFormType() {
        return FormType.CREATE_FOUND_REQUEST;
    }

    @Override
    public BotApiMethod<?> handle(FormData data) {
        log.debug("Create Found Request Handled: {}", data);

        Long finderId = userService.findById(data.userId()).id();
        List<FormStep> steps = FormType.CREATE_FOUND_REQUEST.steps();

        // Step 0: Pet type (required)
        PetType petType = PetType.valueOf(data.choice(steps.get(0)).toUpperCase());

        // Step 1: Photo (required)
        String photoUrl = data.photo(steps.get(1));

        // Step 2: Location (required)
        var location = data.location(steps.get(2));
        Point point = createPoint(location.latitude(), location.longitude());

        // Steps 3-8: Build description from optional text fields
        String description = buildDescription(data, steps);

        // Create found request
        foundRequestService.create(finderId, photoUrl, petType, point, description);

        // Build keyboard with two buttons
        InlineKeyboardMarkup keyboard = buildSuccessKeyboard();

        return ResponseBuilder.sendMessage(data.chatId())
                .text("""
                        ✅ Анкету збережено!

                        Ми знайшли потенційних власників для цієї тварини.

                        ⚠️ Якщо ви натиснете 'Повернутись', переглянути анкети буде неможливо.""")
                .keyboard(keyboard)
                .build();
    }

    private String buildDescription(FormData data, List<FormStep> steps) {
        StringBuilder sb = new StringBuilder();

        // Steps 3-5: Text fields (breed, color, coat type)
        for (int i = 3; i <= 5; i++) {
            String value = data.textOrNull(steps.get(i));
            if (value != null && !value.isBlank()) {
                sb.append(value).append(" ");
            }
        }

        // Step 6: Choice - sex
        String sex = data.choiceOrNull(steps.get(6));
        if (sex != null && !sex.isBlank()) {
            sb.append(sex).append(" ");
        }

        // Step 7: Choice - size
        String size = data.choiceOrNull(steps.get(7));
        if (size != null && !size.isBlank()) {
            sb.append(size).append(" ");
        }

        // Step 8: Text - special features
        String specialFeatures = data.textOrNull(steps.get(8));
        if (specialFeatures != null && !specialFeatures.isBlank()) {
            sb.append(specialFeatures).append(" ");
        }

        return sb.toString().trim();
    }

    private Point createPoint(double latitude, double longitude) {
        GeometryFactory geometryFactory = new GeometryFactory();
        return geometryFactory.createPoint(new Coordinate(longitude, latitude));
    }

    private InlineKeyboardMarkup buildSuccessKeyboard() {
        // Button 1: View recommendations
        String recommendationsCallback = CallbackData.of(CallbackId.LOST_FOUND_MATCHES, null, null).toString();
        InlineKeyboardButton recommendationsButton = InlineKeyboardButton.builder()
                .text("🔍 Переглянути рекомендації")
                .build();
        recommendationsButton.setCallbackData(recommendationsCallback);

        // Button 2: Back to menu
        String backCallback = CallbackData.of(CallbackId.MENU, null, null).toString();
        InlineKeyboardButton backButton = InlineKeyboardButton.builder()
                .text("⬅️ Повернутись")
                .build();
        backButton.setCallbackData(backCallback);

        // Build keyboard with two buttons in one row
        InlineKeyboardRow row = new InlineKeyboardRow(List.of(recommendationsButton, backButton));
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(row))
                .build();
    }
}
