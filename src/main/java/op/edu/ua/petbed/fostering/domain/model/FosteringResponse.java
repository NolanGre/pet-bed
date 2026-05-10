package op.edu.ua.petbed.fostering.domain.model;

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
@Table(name = "fostering_responses")
public class FosteringResponse extends AbstractAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "fostering_responses_seq")
    @SequenceGenerator(name = "fostering_responses_seq", sequenceName = "fostering_responses_seq", allocationSize = 50)
    @Getter(AccessLevel.PRIVATE)
    private @Nullable Long id;

    @Column(name = "fostering_post_id", nullable = false)
    private Long fosteringPostId;

    @Column(name = "user_id", nullable = false)
    private Long responderId;

    @Column(name = "comment", nullable = false)
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FosteringResponseStatus status;

    public static FosteringResponse create(Long fosteringPostId, Long responderId, String comment) {
        return new FosteringResponse(
                null,
                fosteringPostId,
                responderId,
                comment,
                FosteringResponseStatus.NEW
        );
    }

    public long getIdOrThrow() {
        if (id == null) {
            throw new PetBedException("Fostering response is not persisted yet", PetBedException.ErrorCode.ADOPTION_RESPONSE_NOT_PERSISTED);
        }
        return id;
    }

    public void confirmByOwner() {
        if (status != FosteringResponseStatus.NEW) {
            throw new PetBedException("Cannot confirm response that is not new", PetBedException.ErrorCode.ADOPTION_RESPONSE_INVALID_STATUS);
        }
        this.status = FosteringResponseStatus.CONFIRMED_BY_OWNER;
    }

    public void rejectByOwner() {
        if (status != FosteringResponseStatus.NEW && status != FosteringResponseStatus.CONFIRMED_BY_OWNER) {
            throw new PetBedException("Cannot reject response with status: " + status, PetBedException.ErrorCode.ADOPTION_RESPONSE_INVALID_STATUS);
        }
        this.status = FosteringResponseStatus.REJECTED_BY_OWNER;
    }

    public void restore() {
        if (status != FosteringResponseStatus.REJECTED_BY_OWNER) {
            throw new PetBedException("Cannot restore non-rejected response", PetBedException.ErrorCode.ADOPTION_RESPONSE_INVALID_STATUS);
        }
        this.status = FosteringResponseStatus.NEW;
    }

    public void reactivate(String newComment) {
        if (status != FosteringResponseStatus.CANCELLED) {
            throw new PetBedException("Cannot reactivate non-cancelled response", PetBedException.ErrorCode.ADOPTION_RESPONSE_INVALID_STATUS);
        }
        this.status = FosteringResponseStatus.NEW;
        this.comment = newComment;
    }

    public void finalConfirm() {
        if (status != FosteringResponseStatus.CONFIRMED_BY_OWNER) {
            throw new PetBedException("Cannot finalize response that is not confirmed by owner", PetBedException.ErrorCode.ADOPTION_RESPONSE_INVALID_STATUS);
        }
        this.status = FosteringResponseStatus.FINAL_CONFIRMED;
    }

    public void cancel() {
        if (status == FosteringResponseStatus.FINAL_CONFIRMED) {
            throw new PetBedException("Cannot cancel finalized response", PetBedException.ErrorCode.ADOPTION_RESPONSE_INVALID_STATUS);
        }
        this.status = FosteringResponseStatus.CANCELLED;
    }

    public boolean isNew() {
        return status == FosteringResponseStatus.NEW;
    }

    public boolean isConfirmedByOwner() {
        return status == FosteringResponseStatus.CONFIRMED_BY_OWNER;
    }

    public boolean isFinalized() {
        return status == FosteringResponseStatus.FINAL_CONFIRMED;
    }

    @Override
    public final boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        FosteringResponse that = (FosteringResponse) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
