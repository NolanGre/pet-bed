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
        INTERNAL_ERROR("Something went wrong. Please try again later");

        private final String userMessage;
    }
}
