package op.edu.ua.petbed.telegram.form.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.feed.FeedService;
import op.edu.ua.petbed.common.dto.CreateFeedPostDTO;
import op.edu.ua.petbed.telegram.callback.CallbackId;
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
public class CreateFeedPostHandler implements FormSubmissionHandler {

    private final FeedService feedService;
    private final UserService userService;

    @Override
    public FormType getFormType() {
        return FormType.CREATE_FEED_POST;
    }

    @Override
    public BotApiMethod<?> handle(FormData data) {
        log.debug("Create Feed Post Handled: {}", data);

        Long userId = userService.findById(data.userId()).id();
        java.util.List<FormStep> steps = FormType.CREATE_FEED_POST.steps();

        String text = data.text(steps.get(0));
        String photoUrl = data.photo(steps.get(1));
        var location = data.location(steps.get(2));

        CreateFeedPostDTO dto = new CreateFeedPostDTO(
                userId,
                text,
                photoUrl,
                location.latitude(),
                location.longitude()
        );
        feedService.create(dto);

        return ResponseBuilder.sendMessage(data.chatId())
                .text("✅ Публікацію створено!")
                .keyboard(InlineKeyboardBuilder.builder()
                        .backButtonTo(CallbackId.FEED)
                        .build())
                .build();
    }
}