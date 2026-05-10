package op.edu.ua.petbed.telegram.form.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.common.dto.FosteringPostDTO;
import op.edu.ua.petbed.fostering.FosteringPostService;
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
 * Handler for CREATE_FOSTERING_POST form submission.
 * Creates a fostering post for a pet selected by the owner.
 */
@Slf4j
@Component
@NullMarked
@RequiredArgsConstructor
public class CreateFosteringPostHandler implements FormSubmissionHandler {

    private final FosteringPostService fosteringPostService;

    @Override
    public FormType getFormType() {
        return FormType.CREATE_FOSTERING_POST;
    }

    @Override
    public BotApiMethod<?> handle(FormData data) {
        log.debug("Processing CREATE_FOSTERING_POST form for user: {}", data.userId());

        Long petId = data.entityIdOrThrow();
        List<FormStep> steps = FormType.CREATE_FOSTERING_POST.steps();

        // Step 0: Comment (optional)
        @Nullable String comment = data.textOrNull(steps.get(0));

        // Step 1: Duration in days (required, validated as number > 0)
        int durationDays = data.number(steps.get(1));

        log.debug("Creating fostering post for pet: {}, duration: {} days, comment: {}",
                petId, durationDays, comment);
        FosteringPostDTO post = fosteringPostService.create(petId, durationDays, comment);
        log.info("Created fostering post with id: {} for pet: {}", post.id(), petId);

        return ResponseBuilder.sendMessage(data.chatId())
                .text("✅ Тварину передано на перетримку!")
                .keyboard(InlineKeyboardBuilder.builder()
                        .backButtonTo(CallbackId.FOSTERING_MY_POSTS)
                        .build())
                .build();
    }
}