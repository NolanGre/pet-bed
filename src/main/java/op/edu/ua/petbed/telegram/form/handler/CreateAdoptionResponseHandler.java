package op.edu.ua.petbed.telegram.form.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.adoption.AdoptionResponseService;
import op.edu.ua.petbed.adoption.application.dto.AdoptionResponseDTO;
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
 * Handler for CREATE_ADOPTION_RESPONSE form submission.
 * Creates a response to an adoption post from an interested user.
 */
@Slf4j
@Component
@NullMarked
@RequiredArgsConstructor
public class CreateAdoptionResponseHandler implements FormSubmissionHandler {

    private final AdoptionResponseService adoptionResponseService;

    @Override
    public FormType getFormType() {
        return FormType.CREATE_ADOPTION_RESPONSE;
    }

    @Override
    public BotApiMethod<?> handle(FormData data) {
        log.debug("Processing CREATE_ADOPTION_RESPONSE form for user: {}", data.userId());

        // Get the postId from entityId (passed when form is started)
        Long postId = data.entityIdOrThrow();

        // Get form steps
        List<FormStep> steps = FormType.CREATE_ADOPTION_RESPONSE.steps();

        // Step 0: Comment (optional)
        @Nullable String comment = data.textOrNull(steps.get(0));
        if (comment == null) {
            comment = "";
        }

        // Create adoption response
        log.debug("Creating adoption response for post: {}, user: {}, comment: {}",
                postId, data.userId(), comment);
        AdoptionResponseDTO response = adoptionResponseService.create(postId, data.userId(), comment);
        log.info("Created adoption response with id: {} for post: {}", response.id(), postId);

        // Return success message with back button
        return ResponseBuilder.sendMessage(data.chatId())
                .text("✅ Відгук надіслано! Власник побачить вашу заявку та зможе зв'язатися з вами.")
                .keyboard(InlineKeyboardBuilder.builder()
                        .backButtonTo(data.returnCallback())
                        .build())
                .build();
    }
}
