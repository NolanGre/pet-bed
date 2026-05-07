package op.edu.ua.petbed.adoption.domain.model;

import jakarta.persistence.*;
import lombok.*;
import op.edu.ua.petbed.common.model.AbstractAuditableEntity;
import org.jspecify.annotations.NullMarked;

import java.time.Instant;

@NullMarked
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, onConstructor_ = @SuppressWarnings("NullAway"))
@AllArgsConstructor
@Entity
@Table(name = "adoption_view_history")
@IdClass(AdoptionViewHistoryId.class)
public class AdoptionViewHistory extends AbstractAuditableEntity {

    @Id
    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "viewed_at", nullable = false)
    private Instant viewedAt;

    /**
     * Factory method to create a new AdoptionViewHistory.
     */
    public static AdoptionViewHistory create(Long postId, Long userId) {
        return new AdoptionViewHistory(
                postId,
                userId,
                Instant.now()
        );
    }
}
