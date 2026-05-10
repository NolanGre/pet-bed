package op.edu.ua.petbed.fostering.domain.model;

import lombok.*;
import org.jspecify.annotations.NullMarked;

import java.io.Serializable;

@NullMarked
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, onConstructor_ = @SuppressWarnings("NullAway"))
@AllArgsConstructor
@EqualsAndHashCode
public class FosteringSavedPostId implements Serializable {

    private Long postId;
    private Long userId;
}
