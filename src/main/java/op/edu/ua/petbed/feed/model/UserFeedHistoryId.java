package op.edu.ua.petbed.feed.model;

import lombok.*;
import org.jspecify.annotations.NullMarked;

import java.io.Serializable;

@NullMarked
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, onConstructor_ = @SuppressWarnings("NullAway"))
@AllArgsConstructor
@EqualsAndHashCode
public class UserFeedHistoryId implements Serializable {

    private Long userId;
    private Long postId;
}