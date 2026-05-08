package op.edu.ua.petbed.telegram.callback.handler.adoption;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.adoption.AdoptionPostService;
import op.edu.ua.petbed.common.dto.AdoptionRecommendationDTO;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/**
 * Handler for ADOPTION_GET callback - shows adoption posts in the feed with pagination via offset.
 */
@NullMarked
@Component
@RequiredArgsConstructor
@Slf4j
public class AdoptionGetCallbackHandler implements CallbackHandler {

    private final AdoptionPostService adoptionPostService;
    private final TelegramClient telegramClient;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.ADOPTION_GET;
    }

    @Override
    public PartialBotApiMethod<?> handle(CallbackQueryContext context) {
        Long userId = context.auth().userInternalId();
        var callbackData = context.callbackData();
        int offset = callbackData.offset() != null ? callbackData.offset() : 0;
        Long entityId = callbackData.entityId();

        log.info("ADOPTION_GET handle: userId={}, offset={}, entityId={}", userId, offset, entityId);

        if (offset == 0) {
            adoptionPostService.resetOffset(userId);
        }

        AdoptionRecommendationDTO post = adoptionPostService.findNextForFeed(userId, offset);

        log.info("ADOPTION_GET findNextForFeed result: post={}", post != null ? post.postId() : null);

        if (post == null) {
            return noPostsExistMessage(context);
        }

        adoptionPostService.recordView(post.postId(), userId);

        return mapToResponse(context, post, offset);
    }

    private PartialBotApiMethod<?> mapToResponse(CallbackQueryContext context, AdoptionRecommendationDTO post, int offset) {
        log.info("mapToResponse: postId={}, offset={}", post.postId(), offset);

        removeKeyboardFromCurrentMessage(context.chatId(), context.messageId());

        var response = ResponseBuilder.sendPhoto(context.chatId(), post.petPhotoUrl())
                .caption(formatPostInfo(post))
                .keyboard(buildKeyboard(post, offset))
                .build();

        log.info("mapToResponse: returning SendPhoto with keyboard offset={}", offset + 1);
        return response;
    }

    private void removeKeyboardFromCurrentMessage(Long chatId, Integer messageId) {
        try {
            telegramClient.execute(EditMessageReplyMarkup.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .replyMarkup(InlineKeyboardMarkup.builder().build())
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to remove keyboard from message {}: {}", messageId, e.getMessage());
        }
    }

    private EditMessageText noPostsExistMessage(CallbackQueryContext context) {
        return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                .text("""
                        📭 Немає доступних оголошень.

                        Зараз немає тварин для адопції.
                        Перевірте пізніше!
                        """)
                .keyboard(InlineKeyboardBuilder.builder()
                        .backButtonTo(CallbackId.ADOPTION)
                        .build())
                .build();
    }

    private String formatPostInfo(AdoptionRecommendationDTO post) {
        StringBuilder text = new StringBuilder();

        text.append("🐾 ").append(post.petName()).append("\n");

        if (!post.petBreed().isBlank()) {
            text.append("🏷️ Порода: ").append(post.petBreed()).append("\n");
        }

        if (!post.petColor().isBlank()) {
            text.append("🎨 Колір: ").append(post.petColor()).append("\n");
        }

        text.append("🎂 Вік: ").append(formatAge(post.petAge())).append("\n");

        String sexEmoji = switch (post.petSex()) {
            case MALE -> "♂️";
            case FEMALE -> "♀️";
        };
        String sexText = switch (post.petSex()) {
            case MALE -> "він";
            case FEMALE -> "вона";
        };
        text.append(sexEmoji).append(" Стать: ").append(sexText).append("\n");

        String sizeText = switch (post.petSize()) {
            case SMALL -> "малий";
            case MEDIUM -> "середній";
            case LARGE -> "великий";
        };
        text.append("📏 Розмір: ").append(sizeText).append("\n");

        if (post.petSpecialMarks() != null && !post.petSpecialMarks().isBlank()) {
            text.append("📝 Особливі прикмети: ").append(post.petSpecialMarks()).append("\n");
        }

        if (post.ownerComment() != null && !post.ownerComment().isBlank()) {
            text.append("\n💬 ").append(post.ownerComment()).append("\n");
        }

        return text.toString();
    }

    private String formatAge(int age) {
        int lastDigit = age % 10;
        int lastTwoDigits = age % 100;
        if (lastDigit == 1 && lastTwoDigits != 11) {
            return age + " рік";
        } else if (lastDigit >= 2 && lastDigit <= 4 && (lastTwoDigits < 12 || lastTwoDigits > 14)) {
            return age + " роки";
        } else {
            return age + " років";
        }
    }

    private InlineKeyboardMarkup buildKeyboard(AdoptionRecommendationDTO post, int offset) {
        int nextOffset = offset + 1;
        log.info("buildKeyboard: postId={}, currentOffset={}, nextOffset={}", post.postId(), offset, nextOffset);

        return InlineKeyboardBuilder.builder()
                .addButton("➡️ Наступна", CallbackId.ADOPTION_GET, post.postId(), nextOffset)
                .navButtonsFor(CallbackId.ADOPTION_GET, post.postId(), child -> {
                    if (child == CallbackId.ADOPTION_SAVE_POST) return !post.isSaved();
                    if (child == CallbackId.ADOPTION_UNSAVE_POST) return post.isSaved();
                    return false;
                })
                .backButtonTo(CallbackId.ADOPTION)
                .build();
    }
}
