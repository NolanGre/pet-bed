package op.edu.ua.petbed.fostering.domain.model;

import jakarta.persistence.*;
import lombok.*;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.common.model.AbstractAuditableEntity;
import org.hibernate.proxy.HibernateProxy;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Objects;

@NullMarked
@ToString
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, onConstructor_ = @SuppressWarnings("NullAway"))
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "fostering_posts")
public class FosteringPost extends AbstractAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "fostering_posts_seq")
    @SequenceGenerator(name = "fostering_posts_seq", sequenceName = "fostering_posts_seq", allocationSize = 50)
    @Getter(AccessLevel.PRIVATE)
    private @Nullable Long id;

    @Column(name = "pet_id", nullable = false, unique = true)
    private Long petId;

    @Column(name = "planned_duration_days")
    private Integer plannedDurationDays;

    @Column(name = "comment")
    private @Nullable String ownerComment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FosteringPostStatus status;

    @Column(name = "pending_response_id")
    private @Nullable Long pendingResponseId;

    @Column(name = "started_at")
    private @Nullable Instant startedAt;

    @Column(name = "temp_owner_id")
    private @Nullable Long tempOwnerId;

    @Column(name = "expires_at")
    private @Nullable Instant expiresAt;

    public static FosteringPost create(Long petId, Integer plannedDurationDays, @Nullable String ownerComment) {
        return new FosteringPost(
                null,
                petId,
                plannedDurationDays,
                ownerComment,
                FosteringPostStatus.ACTIVE,
                null,
                null,
                null,
                null
        );
    }

    public long getIdOrThrow() {
        if (id == null) {
            throw new PetBedException("Fostering post is not persisted yet", PetBedException.ErrorCode.ADOPTION_POST_NOT_PERSISTED);
        }
        return id;
    }

    public void confirmOwner(Long responseId) {
        if (status != FosteringPostStatus.ACTIVE) {
            throw new PetBedException("Cannot confirm response for non-active post", PetBedException.ErrorCode.ADOPTION_POST_INVALID_STATUS);
        }
        this.pendingResponseId = responseId;
        this.status = FosteringPostStatus.PENDING_CONFIRMATION;
    }

    public void complete(Long tempOwnerId) {
        if (status != FosteringPostStatus.PENDING_CONFIRMATION) {
            throw new PetBedException("Cannot complete fostering that is not pending confirmation", PetBedException.ErrorCode.ADOPTION_POST_INVALID_STATUS);
        }
        this.status = FosteringPostStatus.COMPLETED;
        this.tempOwnerId = tempOwnerId;
        this.startedAt = Instant.now();
        if (plannedDurationDays != null) {
            this.expiresAt = startedAt.plusSeconds(plannedDurationDays * 24L * 60 * 60);
        }
    }

    public void returnToOwner() {
        if (status != FosteringPostStatus.COMPLETED) {
            throw new PetBedException("Cannot return pet that is not in fostering", PetBedException.ErrorCode.ADOPTION_POST_INVALID_STATUS);
        }
        this.status = FosteringPostStatus.ACTIVE;
        this.pendingResponseId = null;
        this.tempOwnerId = null;
        this.startedAt = null;
        this.expiresAt = null;
    }

    public boolean isActive() {
        return status == FosteringPostStatus.ACTIVE;
    }

    public boolean isPendingConfirmation() {
        return status == FosteringPostStatus.PENDING_CONFIRMATION;
    }

    public boolean isCompleted() {
        return status == FosteringPostStatus.COMPLETED;
    }

    public void resetToActive() {
        if (status != FosteringPostStatus.PENDING_CONFIRMATION) {
            throw new PetBedException("Cannot reset post that is not pending confirmation",
                    PetBedException.ErrorCode.ADOPTION_POST_INVALID_STATUS);
        }
        this.status = FosteringPostStatus.ACTIVE;
        this.pendingResponseId = null;
    }

    @Override
    public final boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        FosteringPost that = (FosteringPost) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
