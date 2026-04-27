package op.edu.ua.petbed.telegram.callback.handler.feed;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.common.dto.FeedPostDTO;
import op.edu.ua.petbed.feed.FeedService;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.response.CallbackListItem;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.KeyboardLayout;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import op.edu.ua.petbed.telegram.service.TelegramMessageService;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@NullMarked
@Component
@RequiredArgsConstructor
public class FeedMyPostsCallbackHandler implements CallbackHandler {

    private final FeedService feedService;
    private final TelegramMessageService messageService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.FEED_MY_POSTS;
    }

    @Override
    public PartialBotApiMethod<?> handle(CallbackQueryContext context) {
        var callbackData = context.callbackData();

        if (!context.auth().isVolunteer()) {
            return onlyVolunteersCanDoThis(context);
        }

        int offset = callbackData.offset() != null ? callbackData.offset() : 0;
        var result = feedService.findMyPosts(context.auth().userInternalId(),
                PageRequest.of(offset, KeyboardLayout.DEFAULT.pageSize()));

        if (result.isEmpty()) {
            return noPostsExist(context);
        }

        SendMessage message = ResponseBuilder.sendMessage(context.chatId())
                .text("📋 Мої оголошення")
                .keyboard(buildKeyboard(context, result))
                .build();

        return messageService.editOrReplace(context, message);
    }

    private EditMessageText noPostsExist(CallbackQueryContext context) {
        return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                .text("У вас поки що немає публікацій")
                .keyboard(InlineKeyboardBuilder.builder()
                        .backButtonFor(CallbackId.FEED)
                        .build())
                .build();
    }

    private SendMessage onlyVolunteersCanDoThis(CallbackQueryContext context) {
        return ResponseBuilder.sendMessage(context.chatId())
                .text("Тільки волонтери можуть переглядати публікації")
                .keyboard(InlineKeyboardBuilder.builder()
                        .backButtonFor(CallbackId.FEED)
                        .build())
                .build();
    }

    private InlineKeyboardMarkup buildKeyboard(CallbackQueryContext context, Page<FeedPostDTO> result) {
        return InlineKeyboardBuilder.builder()
                .paginatedList(toPageDto(result), context.callbackData())
                .navButtonsFor(getCallbackId())
                .backButtonFor(getCallbackId())
                .build();
    }

    private Page<CallbackListItem> toPageDto(Page<FeedPostDTO> result) {
        return result.map(post -> new CallbackListItem(
                CallbackId.FEED_POST_DETAIL,
                post.id(),
                post.text().length() > 30
                        ? post.text().substring(0, 30) + "..."
                        : post.text()
        ));
    }
}