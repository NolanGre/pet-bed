package op.edu.ua.petbed.telegram.callback.handler.adoption;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.adoption.AdoptionPostService;
import op.edu.ua.petbed.adoption.dto.AdoptionPostDTO;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;

import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;

import java.util.List;

/**
 * Handler for ADOPTION_MY_POSTS callback - shows list of owner's adoption posts.
 */
@NullMarked
@Component
@RequiredArgsConstructor
public class AdoptionMyPostsCallbackHandler implements CallbackHandler {

    private final AdoptionPostService adoptionPostService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.ADOPTION_MY_POSTS;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        Long userId = context.auth().userInternalId();

        // Get all posts for this owner
        List<AdoptionPostDTO> posts = adoptionPostService.findAllByOwnerId(userId);

        if (posts.isEmpty()) {
            return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                    .text("""
                            📋 У вас ще немає оголошень про передачу тварин.
                            
                            Створіть перше оголошення!
                            """)
                    .keyboard(InlineKeyboardBuilder.builder()
                            .addButton("🤝 Віддати тварину", CallbackId.ADOPTION_GIVE)
                            .backButtonFor(CallbackId.ADOPTION)
                            .build())
                    .build();
        }

        // Build keyboard with post list
        InlineKeyboardBuilder builder = InlineKeyboardBuilder.builder();

        posts.forEach(post -> {
            String statusEmoji = getStatusEmoji(post.status());
            builder.addButton(
                    statusEmoji + " " + post.petName(),
                    CallbackId.ADOPTION_POST_DETAIL, post.id()
            );
        });

        builder.backButtonFor(CallbackId.ADOPTION);

        return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                .text("📋 Ваші оголошення про передачу:")
                .keyboard(builder.build())
                .build();
    }

    private String getStatusEmoji(op.edu.ua.petbed.adoption.domain.model.enums.AdoptionPostStatus status) {
        return switch (status) {
            case ACTIVE -> "🟢";
            case PENDING_CONFIRMATION -> "⏳";
            case COMPLETED -> "✅";
            case CANCELLED -> "❌";
        };
    }
}
