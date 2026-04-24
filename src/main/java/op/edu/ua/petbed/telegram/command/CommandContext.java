package op.edu.ua.petbed.telegram.command;

import op.edu.ua.petbed.telegram.auth.UserAuthContext;
import org.jspecify.annotations.NullMarked;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

/**
 * Context record for incoming command message.
 * <p>
 * Contains all information needed to handle a command:
 * <ul>
 *     <li>{@link #command()} - parsed command enum</li>
 *     <li>{@link #chatId()} - chat where command was sent</li>
 *     <li>{@link #userAuthContext()} - auth with all auth information</li>
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
        UserAuthContext userAuthContext,
        String rawText
) {
    /**
     * Creates auth from Telegram Update containing message.
     *
     * @param update      with Message
     * @param command     parsed Command enum
     * @param authContext fetch user from DB
     * @return parsed CommandContext
     */
    public static CommandContext from(Update update, Command command, UserAuthContext authContext) {
        var msg = update.getMessage();
        return new CommandContext(
                command,
                update,
                msg,
                msg.getChatId(),
                authContext,
                msg.getText()
        );
    }
}