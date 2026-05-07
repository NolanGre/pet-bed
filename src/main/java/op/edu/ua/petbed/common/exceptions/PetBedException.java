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
        INTERNAL_ERROR("🥶 Ой ой, щось пішло не так"),
        USER_TELEGRAM_ID_REQUIRED("Telegram ID is required to create a user"),
        USER_TELEGRAM_USERNAME_REQUIRED("Telegram username is required to create a user"),
        USER_NOT_PERSISTED("User is not persisted yet"),
        USER_NOT_FOUND("User not found"),
        AUTHORIZATION_REQUIRED("This action requires volunteer status"),
        UNKNOWN_COMMAND("Unknown command. Use /help"),
        INVALID_CALLBACK("Invalid callback data"),
        INVALID_FORM_INPUT("❌ Непідтримуваний тип повідомлення."),
        PET_NOT_PERSISTED("Pet is not persisted yet"),
        PET_NOT_FOUND("Pet not found"),
        PET_CANNOT_UPDATE("Cannot update pet with non-default status"),
        PET_CANNOT_DELETE("Cannot delete pet in fostered status"),
        FEED_POST_NOT_FOUND("Post not found"),
        FEED_ACCESS_DENIED("You cannot delete this post"),
        LOST_REQUEST_NOT_PERSISTED("Lost request is not persisted yet"),
        LOST_REQUEST_NOT_FOUND("Lost request not found"),
        LOST_REQUEST_INVALID_INPUT("Invalid input for lost request"),
        LOST_REQUEST_FINDER_REQUIRED("Finder is required"),
        LOST_REQUEST_PHOTO_URL_REQUIRED("Photo URL is required"),
        LOST_REQUEST_PET_TYPE_REQUIRED("Pet type is required"),
        LOST_REQUEST_LOCATION_REQUIRED("Location is required"),
        LOST_REQUEST_DESCRIPTION_REQUIRED("Description is required"),
        LOST_REQUEST_REQUIRED("Lost request is required"),
        FOUND_REQUEST_REQUIRED("Found request is required"),
        FOUND_REQUEST_NOT_FOUND("Found request not found"),
        MATCH_SCORE_REQUIRED("Score is required"),
        MATCH_QUEUE_ENTRY_NOT_PERSISTED("Match queue entry is not persisted yet"),
        VIEWED_BY_REQUIRED("ViewedBy is required"),

        // Adoption module
        ADOPTION_POST_NOT_PERSISTED("Adoption post is not persisted yet"),
        ADOPTION_POST_NOT_FOUND("Adoption post not found"),
        ADOPTION_POST_INVALID_STATUS("Invalid adoption post status for this operation"),
        ADOPTION_POST_ALREADY_EXISTS("Active adoption post already exists for this pet"),
        ADOPTION_RESPONSE_NOT_PERSISTED("Adoption response is not persisted yet"),
        ADOPTION_RESPONSE_NOT_FOUND("Adoption response not found"),
        ADOPTION_RESPONSE_INVALID_STATUS("Invalid adoption response status for this operation"),
        ADOPTION_RESPONSE_ALREADY_EXISTS("You have already responded to this post"),
        ADOPTION_RESPONSE_NOT_AUTHORIZED("You are not authorized to perform this action on this response"),
        ADOPTION_RESPONSE_SELF_RESPONSE("Cannot respond to your own post"),
        ADOPTION_RESPONSE_NOT_FINALIZED("Response must be finalized before completing adoption"),
        ADOPTION_POST_NOT_AUTHORIZED("You are not authorized to perform this action on this post"),
        ADOPTION_UNAUTHORIZED("You are not authorized to perform this action on this adoption post"),
        ADOPTION_SAVED_POST_NOT_FOUND("Saved post not found");

        private final String userMessage;
    }
}
