package op.edu.ua.petbed.telegram.form.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.common.model.PetType;
import op.edu.ua.petbed.lost.FoundRequestService;
import op.edu.ua.petbed.lost.application.dto.CreateFoundRequestDTO;
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

        // Build DTO using builder pattern
        CreateFoundRequestDTO dto = CreateFoundRequestDTO.builder()
                .finderId(finderId)
                .photoUrl(photoUrl)
                .petType(petType)
                .location(point)
                .breed(data.textOrNull(steps.get(3)))
                .color(data.textOrNull(steps.get(4)))
                .coat(data.textOrNull(steps.get(5)))
                .sex(data.choiceOrNull(steps.get(6)))
                .size(data.choiceOrNull(steps.get(7)))
                .features(data.textOrNull(steps.get(8)))
                .build();

        // Create found request
        foundRequestService.create(dto);

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
