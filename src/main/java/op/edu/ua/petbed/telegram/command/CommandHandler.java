package op.edu.ua.petbed.telegram.command;

import org.jspecify.annotations.NullMarked;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;

/**
 * Handler for bot commands.
 * <p>
 * When user sends a command (e.g., /start), router dispatches to appropriate
 * CommandHandler based on {@link Command} enum.
 * @see Command
 * @see CommandContext
 */
@NullMarked
public interface CommandHandler {

    /**
     * Returns the command this handler handles.
     *
     * @return Command enum value
     */
    Command getCommand();

    /**
     * Handles the command.
     *
     * @param context containing message and user info
     * @return PartialBotApiMethod response (text, media, or other Telegram API method)
     */
    PartialBotApiMethod<?> handle(CommandContext context);
}