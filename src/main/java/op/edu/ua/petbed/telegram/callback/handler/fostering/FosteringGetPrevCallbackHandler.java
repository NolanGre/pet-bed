package op.edu.ua.petbed.telegram.callback.handler.fostering;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.common.dto.FosteringRecommendationDTO;
import op.edu.ua.petbed.fostering.FosteringPostService;
import op.edu.ua.petbed.fostering.FosteringSavedPostService;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import op.edu.ua.petbed.telegram.service.TelegramMessageService;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@NullMarked
@Component
@RequiredArgsConstructor
@Slf4j
public class FosteringGetPrevCallbackHandler implements CallbackHandler {

    private final FosteringPostService fosteringPostService;
    private final FosteringSavedPostService fosteringSavedPostService;
    private final TelegramMessageService messageService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.FOSTERING_GET_PREV;
    }

    @Override
    public PartialBotApiMethod<?> handle(CallbackQueryContext context) {
        Long userId = context.auth().userInternalId();

        @Nullable FosteringRecommendationDTO post = fosteringPostService.findPreviousFromHistory(userId);

        if (post == null) {
            return noMoreHistoryMessage(context);
        }

        fosteringPostService.recordView(post.postId(), userId);
        return mapToResponse(context, post);
    }

    private PartialBotApiMethod<?> mapToResponse(CallbackQueryContext context, FosteringRecommendationDTO post) {
        String caption = formatCaption(post);
        InlineKeyboardMarkup keyboard = buildKeyboard(post, context);

        return ResponseBuilder.editPhoto(context.chatId(), context.messageId(), post.petPhotoUrl())
                .caption(caption)
                .keyboard(keyboard)
                .build();
    }

    private String formatCaption(FosteringRecommendationDTO post) {
        StringBuilder text = new StringBuilder();
        text.append("🐾 ").append(post.petName()).append("\n\n");

        if (post.plannedDurationDays() != null) {
            text.append("📅 Тривалість: ").append(post.plannedDurationDays()).append(" днів\n\n");
        }

        if (post.petBreed() != null) {
            text.append("🐕 Порода: ").append(post.petBreed()).append("\n");
        }
        if (post.petColor() != null) {
            text.append("🎨 Колір: ").append(post.petColor()).append("\n");
        }
        if (post.petAge() != null) {
            text.append("🎂 Вік: ").append(post.petAge()).append("років\n");
        }

        if (post.ownerComment() != null && !post.ownerComment().isBlank()) {
            text.append("\n💬 ").append(post.ownerComment());
        }

        return text.toString();
    }

    private InlineKeyboardMarkup buildKeyboard(FosteringRecommendationDTO post, CallbackQueryContext context) {
        Long userId = context.auth().userInternalId();
        boolean isSaved = fosteringSavedPostService.isSaved(post.postId(), userId);

        return InlineKeyboardBuilder.builder()
                .addButton(isSaved ? "💔 Видалити" : "❤️ Зберегти",
                        isSaved ? CallbackId.FOSTERING_UNSAVE_POST : CallbackId.FOSTERING_SAVE_POST,
                        post.postId())
                .addButton("✉️ Відгукнутись", CallbackId.FOSTERING_RESPONSE_CREATE, post.postId())
                .addButton("◀️ Минула", CallbackId.FOSTERING_GET_PREV, post.postId())
                .addButton("➡️ Наступна", CallbackId.FOSTERING_GET_NEXT, post.postId())
                .backButtonTo(CallbackId.FOSTERING)
                .build();
    }

    private EditMessageText noMoreHistoryMessage(CallbackQueryContext context) {
        return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                .text("""
                        📭 Ви дійшли до початку історії перегляду.

                        Більше немає попередніх анкет.
                        """)
                .keyboard(InlineKeyboardBuilder.builder()
                        .addButton("➡️ Наступна", CallbackId.FOSTERING_GET_NEXT)
                        .backButtonTo(CallbackId.FOSTERING)
                        .build())
                .build();
    }
}