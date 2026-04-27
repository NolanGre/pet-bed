package op.edu.ua.petbed.common.dto;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

@NullMarked
public record FeedPostDTO(
        Long id,
        Long publisherId,
        String publisherUsername,
        String text,
        String photoUrl,
        double latitude,
        double longitude,
        @Nullable Double distance,
        Instant createdAt
) {
}