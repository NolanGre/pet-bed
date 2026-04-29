package op.edu.ua.petbed.telegram.form.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.lost.LostRequestService;
import op.edu.ua.petbed.common.dto.LostRequestDTO;
import op.edu.ua.petbed.pet.PetService;
import op.edu.ua.petbed.telegram.form.FormData;
import op.edu.ua.petbed.telegram.form.scheme.FormInput;
import op.edu.ua.petbed.telegram.form.scheme.FormStep;
import op.edu.ua.petbed.telegram.form.scheme.FormType;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import op.edu.ua.petbed.user.UserService;
import org.jspecify.annotations.NullMarked;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;

import java.util.List;

@Slf4j
@Component
@NullMarked
@RequiredArgsConstructor
public class CreateLostRequestHandler implements FormSubmissionHandler {

    private final LostRequestService lostRequestService;
    private final PetService petService;
    private final UserService userService;

    @Override
    public FormType getFormType() {
        return FormType.CREATE_LOST_REQUEST;
    }

    @Override
    public BotApiMethod<?> handle(FormData data) {
        log.debug("Processing CREATE_LOST_REQUEST form for user: {}", data.userId());

        // Get the petId from entityId (passed when form is started)
        Long petId = data.entityIdOrThrow();

        // Get form steps
        List<FormStep> steps = FormType.CREATE_LOST_REQUEST.steps();

        // Step 0: Contact info (required)
        String contactInfo = data.text(steps.get(0));

        // Step 1: Location (required)
        FormInput.Location location = data.location(steps.get(1));
        Point point = createPoint(location.latitude(), location.longitude());

        // Step 2: Special features (optional)
        String specialFeatures = data.textOrNull(steps.get(2));

        // If special features provided, update pet
        if (specialFeatures != null && !specialFeatures.isBlank()) {
            log.debug("Updating special features for pet {}: {}", petId, specialFeatures);
            petService.updateSpecialFeatures(petId, specialFeatures);
        }

        // Create lost request
        log.debug("Creating lost request for pet: {}, contact: {}", petId, contactInfo);
        LostRequestDTO lostRequest = lostRequestService.create(petId, contactInfo, point);
        log.info("Created lost request with id: {} for pet: {}", lostRequest.id(), petId);

        // Return success message with back button
        return ResponseBuilder.sendMessage(data.chatId())
                .text("✅ Пошук запущено! Ми повідомимо вас про можливі збіги.")
                .keyboard(InlineKeyboardBuilder.builder()
                        .backButtonTo(data.returnCallback())
                        .build())
                .build();
    }

    private Point createPoint(double latitude, double longitude) {
        GeometryFactory geometryFactory = new GeometryFactory();
        return geometryFactory.createPoint(new Coordinate(longitude, latitude));
    }
}
