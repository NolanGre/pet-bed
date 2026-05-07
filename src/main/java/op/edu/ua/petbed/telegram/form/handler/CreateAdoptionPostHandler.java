package op.edu.ua.petbed.telegram.form.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.adoption.AdoptionPostService;
import op.edu.ua.petbed.adoption.application.dto.AdoptionPostDTO;
import op.edu.ua.petbed.telegram.form.FormData;
import op.edu.ua.petbed.telegram.form.scheme.FormStep;
import op.edu.ua.petbed.telegram.form.scheme.FormType;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;

import java.util.List;

/**
 * Handler for CREATE_ADOPTION_POST form submission.
 * Creates an adoption post for a pet selected by the owner.
 */
@Slf4j
@Component
@NullMarked
@RequiredArgsConstructor
public class CreateAdoptionPostHandler implements FormSubmissionHandler {

    private final AdoptionPostService adoptionPostService;

    @Override
    public FormType getFormType() {
        return FormType.CREATE_ADOPTION_POST;
    }

    @Override
    public BotApiMethod<?> handle(FormData data) {
        log.debug("Processing CREATE_ADOPTION_POST form for user: {}", data.userId());

        // Get the petId from entityId (passed when form is started)
        Long petId = data.entityIdOrThrow();

        // Get form steps
        List<FormStep> steps = FormType.CREATE_ADOPTION_POST.steps();

        // Step 0: Owner comment (optional)
        @Nullable String ownerComment = data.textOrNull(steps.get(0));

        // Create adoption post
        log.debug("Creating adoption post for pet: {}, comment: {}", petId, ownerComment);
        AdoptionPostDTO post = adoptionPostService.create(petId, ownerComment);
        log.info("Created adoption post with id: {} for pet: {}", post.id(), petId);

        // Return success message with back button
        return ResponseBuilder.sendMessage(data.chatId())
                .text("✅ Анкету на передачу тварини створено! Охочі зможуть залишати відгуки.")
                .keyboard(InlineKeyboardBuilder.builder()
                        .backButtonTo(data.returnCallback())
                        .build())
                .build();
    }
}
