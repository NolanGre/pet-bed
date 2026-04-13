package op.edu.ua.petbed.telegram.command;

import com.fasterxml.jackson.annotation.JsonValue;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Enum representing available bot commands triggered by /command.
 * <p>
 * Commands are typed messages starting with '/' (e.g., /start, /help).
 * Router parses incoming message and dispatches to appropriate {@link CommandHandler}.
 *
 * @see CommandHandler
 * @see CommandContext
 */
@NullMarked
public enum Command {
    START("/start"),
    MENU("/menu"),
    PROFILE("/profile"),
    PUBLISH("/publish"),
    DEFAULT("");

    private final String label;

    Command(String label) {
        this.label = label;
    }

    /**
     * Returns command label (e.g., "/start").
     * Used for parsing incoming messages.
     *
     * @return command string with leading /
     */
    @JsonValue
    public String getLabel() {
        return label;
    }

    public boolean requiresAuth() {
        return this == START || this == PROFILE;
    }

    public boolean requiresVolunteer() {
        return this == PUBLISH;
    }

    /**
     * Parses message text to Command enum.
     * Extracts first word (before space) and matches to enum.
     *
     * @param text message text (e.g., "/start args")
     * @return matching Command or DEFAULT
     */
    public static Command fromLabel(@Nullable String text) {
        if (text == null || text.isBlank()) {
            return DEFAULT;
        }
        String cmd = text.trim().split(" ")[0];
        for (Command c : Command.values()) {
            if (c.label.equalsIgnoreCase(cmd)) {
                return c;
            }
        }
        return DEFAULT;
    }
}