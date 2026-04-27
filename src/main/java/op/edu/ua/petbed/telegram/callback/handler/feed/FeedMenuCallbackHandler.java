package op.edu.ua.petbed.telegram.callback.handler.feed;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.feed.FeedService;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import op.edu.ua.petbed.telegram.service.TelegramMessageService;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import static op.edu.ua.petbed.common.model.UserType.VOLUNTEER;

@NullMarked
@Component
@RequiredArgsConstructor
public class FeedMenuCallbackHandler implements CallbackHandler {

    private final FeedService feedService;
    private final TelegramMessageService telegramMessageService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.FEED;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        return mapToResponse(context);
    }

    private BotApiMethod<?> mapToResponse(CallbackQueryContext context) {
        var auth = context.auth();
        String messageText = """
                📋 Стрічка оголошень
                
                Тут ви можете переглянути оголошення від волонтерів.
                """;


        var response = ResponseBuilder.sendMessage(context.chatId())
                .text(messageText)
                .keyboard(feedKeyboard(auth.userInternalId(), auth.userType() == VOLUNTEER))
                .build();
        return telegramMessageService.editOrSend(context, response);
    }

    private InlineKeyboardMarkup feedKeyboard(Long userId, boolean isVolunteer) {
        boolean hasPosts = feedService.findMyPosts(userId, PageRequest.of(0, 1)).getTotalElements() >= 1;

        return InlineKeyboardBuilder.builder()
                .navButtonsFor(CallbackId.FEED, child -> {
                    if (child == CallbackId.FEED_CREATE) {
                        return isVolunteer;
                    }
                    if (child == CallbackId.FEED_MY_POSTS) {
                        return isVolunteer && hasPosts;
                    }
                    return true;
                })
                .backButtonFor(CallbackId.FEED)
                .build();
    }
}