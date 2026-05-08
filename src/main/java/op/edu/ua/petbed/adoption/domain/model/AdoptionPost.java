package op.edu.ua.petbed.adoption.domain.model;

import jakarta.persistence.*;
import lombok.*;
import op.edu.ua.petbed.common.model.AdoptionPostStatus;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.common.model.AbstractAuditableEntity;
import org.hibernate.proxy.HibernateProxy;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

@NullMarked
@ToString
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, onConstructor_ = @SuppressWarnings("NullAway"))
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "adoption_posts")
public class AdoptionPost extends AbstractAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "adoption_posts_seq")
    @SequenceGenerator(name = "adoption_posts_seq", sequenceName = "adoption_posts_seq", allocationSize = 50)
    @Getter(AccessLevel.PRIVATE)
    private @Nullable Long id;

    @Column(name = "pet_id", nullable = false, unique = true)
    private Long petId;

    @Column(name = "comment")
    private @Nullable String ownerComment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AdoptionPostStatus status;

    @Column(name = "pending_response_id")
    private @Nullable Long pendingResponseId;

    /**
     * Factory method to create a new AdoptionPost.
     */
    public static AdoptionPost create(Long petId, @Nullable String ownerComment) {
        return new AdoptionPost(
                null,
                petId,
                ownerComment,
                AdoptionPostStatus.ACTIVE,
                null
        );
    }

    public long getIdOrThrow() {
        if (id == null) {
            throw new PetBedException("Adoption post is not persisted yet", PetBedException.ErrorCode.ADOPTION_POST_NOT_PERSISTED);
        }
        return id;
    }

    /**
     * Confirms an owner-selected response.
     * Changes status to PENDING_CONFIRMATION and sets pendingResponseId.
     */
    public void confirmOwner(Long responseId) {
        if (status != AdoptionPostStatus.ACTIVE) {
            throw new PetBedException("Cannot confirm response for non-active post", PetBedException.ErrorCode.ADOPTION_POST_INVALID_STATUS);
        }
        this.pendingResponseId = responseId;
        this.status = AdoptionPostStatus.PENDING_CONFIRMATION;
    }

    /**
     * Completes the adoption process.
     * Called after the responder finalizes their confirmation.
     */
    public void complete() {
        if (status != AdoptionPostStatus.PENDING_CONFIRMATION) {
            throw new PetBedException("Cannot complete adoption that is not pending confirmation", PetBedException.ErrorCode.ADOPTION_POST_INVALID_STATUS);
        }
        this.status = AdoptionPostStatus.COMPLETED;
    }

    public boolean isActive() {
        return status == AdoptionPostStatus.ACTIVE;
    }

    public boolean isPendingConfirmation() {
        return status == AdoptionPostStatus.PENDING_CONFIRMATION;
    }

    public void resetToActive() {
        if (status != AdoptionPostStatus.PENDING_CONFIRMATION) {
            throw new PetBedException("Cannot reset post that is not pending confirmation",
                    PetBedException.ErrorCode.ADOPTION_POST_INVALID_STATUS);
        }
        this.status = AdoptionPostStatus.ACTIVE;
        this.pendingResponseId = null;
    }

    @Override
    public final boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        AdoptionPost that = (AdoptionPost) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
