package op.edu.ua.petbed.common.exceptions;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@EqualsAndHashCode(callSuper = true)
public class PetBedException extends RuntimeException {

    private final ErrorCode errorCode;

    public PetBedException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    @Getter
    @RequiredArgsConstructor
    public enum ErrorCode {
        UNSUPPORTED_UPDATE("This type of update is not supported"),
        INTERNAL_ERROR("Something went wrong. Please try again later"),
        USER_TELEGRAM_ID_REQUIRED("Telegram ID is required to create a user"),
        USER_TELEGRAM_USERNAME_REQUIRED("Telegram username is required to create a user"),
        USER_NOT_PERSISTED("User is not persisted yet"),
        USER_NOT_FOUND("User not found"),
        AUTHORIZATION_REQUIRED("This action requires volunteer status"),
        UNKNOWN_COMMAND("Unknown command. Use /help"),
        INVALID_CALLBACK("Invalid callback data"),
        VALIDATION_ERROR("Invalid data");

        private final String userMessage;
    }
}
