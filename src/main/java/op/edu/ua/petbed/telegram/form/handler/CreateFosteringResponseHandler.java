package op.edu.ua.petbed.telegram.form.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.common.dto.FosteringResponseDTO;
import op.edu.ua.petbed.fostering.FosteringResponseService;
import op.edu.ua.petbed.telegram.callback.CallbackId;
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
 * Handler for CREATE_FOSTERING_RESPONSE form submission.
 * Creates a response to a fostering post from an interested user.
 */
@Slf4j
@Component
@NullMarked
@RequiredArgsConstructor
public class CreateFosteringResponseHandler implements FormSubmissionHandler {

    private final FosteringResponseService fosteringResponseService;

    @Override
    public FormType getFormType() {
        return FormType.CREATE_FOSTERING_RESPONSE;
    }

    @Override
    public BotApiMethod<?> handle(FormData data) {
        log.debug("Processing CREATE_FOSTERING_RESPONSE form for user: {}", data.userId());

        // Get the postId from entityId (passed when form is started)
        Long postId = data.entityIdOrThrow();

        // Get form steps
        List<FormStep> steps = FormType.CREATE_FOSTERING_RESPONSE.steps();

        // Step 0: Comment (optional)
        @Nullable String comment = data.textOrNull(steps.get(0));
        if (comment == null) {
            comment = "";
        }

        // Create fostering response
        log.debug("Creating fostering response for post: {}, user: {}, comment: {}",
                postId, data.userId(), comment);
        FosteringResponseDTO response = fosteringResponseService.create(postId, data.userId(), comment);
        log.info("Created fostering response with id: {} for post: {}", response.id(), postId);

        // Return success message with back button
        return ResponseBuilder.sendMessage(data.chatId())
                .text("✅ Відгук надіслано!")
                .keyboard(InlineKeyboardBuilder.builder()
                        .backButtonTo(CallbackId.FOSTERING)
                        .build())
                .build();
    }
}