package op.edu.ua.petbed.adoption.domain.model;

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
@Table(name = "adoption_saved_posts")
@IdClass(AdoptionSavedPostId.class)
public class AdoptionSavedPost {

    @Id
    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "saved_at", nullable = false)
    private Instant savedAt;

    /**
     * Factory method to create a new AdoptionSavedPost.
     */
    public static AdoptionSavedPost create(Long postId, Long userId) {
        return new AdoptionSavedPost(
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
