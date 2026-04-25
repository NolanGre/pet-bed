package op.edu.ua.petbed.telegram.form.scheme;

import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.common.exceptions.PetBedException.ErrorCode;
import org.jspecify.annotations.NullMarked;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@NullMarked
public sealed interface FormInput {

    // For type tag only
    Text TEXT = new Text("");
    Photo PHOTO = new Photo("");
    Location LOCATION = new Location(0, 0);
    Number NUMBER = new Number(0);
    Choice CHOICE = new Choice("");

    record Text(String value) implements FormInput {
    }

    record Photo(String fileId) implements FormInput {
    }

    record Location(double latitude, double longitude) implements FormInput {
    }

    // Subtype of text
    record Number(int value) implements FormInput {
    }

    record Choice(String value) implements FormInput {}

    /**
     * Converts a Telegram message to the appropriate input type.
     */
    static FormInput from(Message message) {
        if (message.hasPhoto()) {
            return new Photo(message.getPhoto().getLast().getFileId());
        }
        if (message.hasLocation()) {
            return new Location(message.getLocation().getLatitude(), message.getLocation().getLongitude());
        }
        if (message.hasText()) {
            String text = message.getText();
            if (text == null || text.isBlank()) {
                throw new PetBedException("Empty text input", ErrorCode.INVALID_FORM_INPUT);
            }
            try {
                return new Number(Integer.parseInt(text.trim()));
            } catch (NumberFormatException e) {
                return new Text(text);
            }
        }
        throw new PetBedException("Unsupported message type", ErrorCode.INVALID_FORM_INPUT);
    }
}
