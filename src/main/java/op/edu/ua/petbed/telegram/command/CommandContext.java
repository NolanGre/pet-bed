package op.edu.ua.petbed.telegram.command;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * Context record for incoming command message.
 * <p>
 * Contains all information needed to handle a command:
 * <ul>
 *     <li>{@link #command()} - parsed command enum</li>
 *     <li>{@link #userId()} - user who sent command</li>
 *     <li>{@link #chatId()} - chat where command was sent</li>
 *     <li>{@link #rawText()} - full message text with arguments</li>
 * </ul>
 *
 * @see Command
 * @see CommandHandler
 */
@NullMarked
public record CommandContext(
    Command command,
    Update update,
    Message message,
    Long chatId,
    Long userId,
    @Nullable String username,
    String rawText
) {
    /**
     * Creates context from Telegram Update containing message.
     *
     * @param update with Message
     * @param command parsed Command enum
     * @return parsed CommandContext
     */
    public static CommandContext from(Update update, Command command) {
        var msg = update.getMessage();
        var from = msg.getFrom();
        return new CommandContext(
            command,
            update,
            msg,
            msg.getChatId(),
            from.getId(),
            from.getUserName(),
            msg.getText()
        );
    }
}