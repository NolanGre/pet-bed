package op.edu.ua.petbed.fostering.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NullMarked;

import java.time.Instant;

@NullMarked
@NoArgsConstructor(access = AccessLevel.PROTECTED, onConstructor_ = @SuppressWarnings("NullAway"))
@AllArgsConstructor
@Entity
@Table(name = "fostering_saved_posts")
@IdClass(FosteringSavedPostId.class)
public class FosteringSavedPost {

    @Id
    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "saved_at", nullable = false)
    private Instant savedAt;

    public static FosteringSavedPost create(Long postId, Long userId) {
        return new FosteringSavedPost(
                postId,
                userId,
                Instant.now()
        );
    }

    public Long getPostId() {
        return postId;
    }

    public Long getUserId() {
        return userId;
    }

    public Instant getSavedAt() {
        return savedAt;
    }
}
