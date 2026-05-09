package op.edu.ua.petbed.adoption.domain.model;

import jakarta.persistence.*;
import lombok.*;
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
@Table(name = "adoption_responses")
public class AdoptionResponse extends AbstractAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "adoption_responses_seq")
    @SequenceGenerator(name = "adoption_responses_seq", sequenceName = "adoption_responses_seq", allocationSize = 50)
    @Getter(AccessLevel.PRIVATE)
    private @Nullable Long id;

    @Column(name = "adoption_post_id", nullable = false)
    private Long adoptionPostId;

    @Column(name = "user_id", nullable = false)
    private Long responderId;

    @Column(name = "comment", nullable = false)
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AdoptionResponseStatus status;

    /**
     * Factory method to create a new AdoptionResponse.
     */
    public static AdoptionResponse create(Long adoptionPostId, Long responderId, String comment) {
        return new AdoptionResponse(
                null,
                adoptionPostId,
                responderId,
                comment,
                AdoptionResponseStatus.NEW
        );
    }

    public long getIdOrThrow() {
        if (id == null) {
            throw new PetBedException("Adoption response is not persisted yet", PetBedException.ErrorCode.ADOPTION_RESPONSE_NOT_PERSISTED);
        }
        return id;
    }

    /**
     * Confirms this response by the owner.
     */
    public void confirmByOwner() {
        if (status != AdoptionResponseStatus.NEW) {
            throw new PetBedException("Cannot confirm response that is not new", PetBedException.ErrorCode.ADOPTION_RESPONSE_INVALID_STATUS);
        }
        this.status = AdoptionResponseStatus.CONFIRMED_BY_OWNER;
    }

    /**
     * Rejects this response by the owner.
     */
    public void rejectByOwner() {
        if (status != AdoptionResponseStatus.NEW && status != AdoptionResponseStatus.CONFIRMED_BY_OWNER) {
            throw new PetBedException("Cannot reject response with status: " + status, PetBedException.ErrorCode.ADOPTION_RESPONSE_INVALID_STATUS);
        }
        this.status = AdoptionResponseStatus.REJECTED_BY_OWNER;
    }

    /**
     * Restores a rejected response to NEW status.
     */
    public void restore() {
        if (status != AdoptionResponseStatus.REJECTED_BY_OWNER) {
            throw new PetBedException("Cannot restore non-rejected response", PetBedException.ErrorCode.ADOPTION_RESPONSE_INVALID_STATUS);
        }
        this.status = AdoptionResponseStatus.NEW;
    }

    /**
     * Reactivates a cancelled response to NEW status with updated comment.
     */
    public void reactivate(String newComment) {
        if (status != AdoptionResponseStatus.CANCELLED) {
            throw new PetBedException("Cannot reactivate non-cancelled response", PetBedException.ErrorCode.ADOPTION_RESPONSE_INVALID_STATUS);
        }
        this.status = AdoptionResponseStatus.NEW;
        this.comment = newComment;
    }

    /**
     * Final confirmation by the responder.
     */
    public void finalConfirm() {
        if (status != AdoptionResponseStatus.CONFIRMED_BY_OWNER) {
            throw new PetBedException("Cannot finalize response that is not confirmed by owner", PetBedException.ErrorCode.ADOPTION_RESPONSE_INVALID_STATUS);
        }
        this.status = AdoptionResponseStatus.FINAL_CONFIRMED;
    }

    /**
     * Cancels this response by the responder.
     */
    public void cancel() {
        if (status == AdoptionResponseStatus.FINAL_CONFIRMED) {
            throw new PetBedException("Cannot cancel finalized response", PetBedException.ErrorCode.ADOPTION_RESPONSE_INVALID_STATUS);
        }
        this.status = AdoptionResponseStatus.CANCELLED;
    }

    public boolean isNew() {
        return status == AdoptionResponseStatus.NEW;
    }

    public boolean isConfirmedByOwner() {
        return status == AdoptionResponseStatus.CONFIRMED_BY_OWNER;
    }

    public boolean isFinalized() {
        return status == AdoptionResponseStatus.FINAL_CONFIRMED;
    }

    @Override
    public final boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        AdoptionResponse that = (AdoptionResponse) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
