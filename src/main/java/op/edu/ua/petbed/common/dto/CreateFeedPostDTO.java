package op.edu.ua.petbed.common.dto;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record CreateFeedPostDTO(
        Long publisherId,
        String text,
        String photoUrl,
        double latitude,
        double longitude
) {
}