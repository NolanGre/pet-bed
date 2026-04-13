package op.edu.ua.petbed.telegram.callback;

/**
 * Enum representing available callback actions triggered by inline keyboard buttons.
 * <p>
 * When user clicks an inline keyboard button, Telegram sends a CallbackQuery with callback_data.
 * This enum maps string callback_data to type-safe actions for handler dispatch.
 */
public enum CallbackAction {
    /** Pagination navigation (next/previous page). Payload: offset integer. */
    PAGINATION,

    /** User confirmed an action. */
    CONFIRM,

    /** User canceled an action. */
    CANCEL,

    /** Unknown or unrecognized action - fallback. */
    DEFAULT;

    /**
     * Parses string to CallbackAction enum.
     *
     * @param action string from callback_data (case-insensitive)
     * @return matching CallbackAction or DEFAULT if not found
     */
    public static CallbackAction fromString(String action) {
        if (action == null || action.isBlank()) {
            return DEFAULT;
        }
        try {
            return valueOf(action.toUpperCase());
        } catch (IllegalArgumentException e) {
            return DEFAULT;
        }
    }
}