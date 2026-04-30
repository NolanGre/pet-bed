package op.edu.ua.petbed.telegram.form.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.common.model.PetType;
import op.edu.ua.petbed.lost.FoundRequestService;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.form.FormData;
import op.edu.ua.petbed.telegram.form.scheme.FormStep;
import op.edu.ua.petbed.telegram.form.scheme.FormType;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import org.jspecify.annotations.NullMarked;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.List;

@Slf4j
@Component
@NullMarked
@RequiredArgsConstructor
public class CreateFoundRequestHandler implements FormSubmissionHandler {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    private final FoundRequestService foundRequestService;

    @Override
    public FormType getFormType() {
        return FormType.CREATE_FOUND_REQUEST;
    }

    @Override
    public BotApiMethod<?> handle(FormData data) {
        log.debug("Create Found Request Handled: {}", data);

        Long finderId = data.userId();
        List<FormStep> steps = FormType.CREATE_FOUND_REQUEST.steps();

        // Step 0: Pet type (required)
        PetType petType;
        try {
            petType = PetType.valueOf(data.choice(steps.get(0)).toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new PetBedException("Invalid pet type", PetBedException.ErrorCode.INVALID_FORM_INPUT);
        }

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
                        
                        ⚠️ Якщо ви натиснете 'Повернутись', переглянути анкети буде неможливо.
                        """)
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
        return GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
    }

    private InlineKeyboardMarkup buildSuccessKeyboard() {
        return InlineKeyboardBuilder.builder()
                .addButton("🔍 Переглянути рекомендації", CallbackId.LOST_FOUND_MATCHES)
                .backButtonTo(CallbackId.MENU)
                .build();
    }
}
