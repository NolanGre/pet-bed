package op.edu.ua.petbed.adoption.domain.model;

import lombok.*;
import org.jspecify.annotations.NullMarked;

import java.io.Serializable;

@NullMarked
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, onConstructor_ = @SuppressWarnings("NullAway"))
@AllArgsConstructor
@EqualsAndHashCode
public class AdoptionSavedPostId implements Serializable {

    private Long postId;
    private Long userId;
}
