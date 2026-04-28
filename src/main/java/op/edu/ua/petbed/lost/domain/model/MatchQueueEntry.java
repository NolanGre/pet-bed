package op.edu.ua.petbed.lost.domain.model;

import jakarta.persistence.*;
import lombok.*;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.common.model.AbstractAuditableEntity;
import org.hibernate.proxy.HibernateProxy;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.Objects;

@NullMarked
@ToString
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, onConstructor_ = @SuppressWarnings("NullAway"))
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "match_queue")
public class MatchQueueEntry extends AbstractAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "match_queue_seq")
    @SequenceGenerator(name = "match_queue_seq", sequenceName = "match_queue_seq", allocationSize = 50)
    @Column(nullable = false)
    @Getter(AccessLevel.PRIVATE)
    private @Nullable Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lost_request_id", nullable = false)
    private LostRequest lostRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "found_request_id", nullable = false)
    private FoundRequest foundRequest;

    @Column(nullable = false, precision = 7, scale = 4)
    private BigDecimal score;

    @Enumerated(EnumType.STRING)
    @Column(name = "viewing_status", nullable = false)
    private ViewingStatus viewingStatus;

    @Column(name = "viewed_by")
    private @Nullable String viewedBy;

    public static MatchQueueEntry create(LostRequest lost, FoundRequest found, BigDecimal score) {
        if (lost == null) {
            throw new PetBedException("LostRequest is required", PetBedException.ErrorCode.LOST_REQUEST_REQUIRED);
        }
        if (found == null) {
            throw new PetBedException("FoundRequest is required", PetBedException.ErrorCode.FOUND_REQUEST_REQUIRED);
        }
        if (score == null) {
            throw new PetBedException("Score is required", PetBedException.ErrorCode.MATCH_SCORE_REQUIRED);
        }
        return new MatchQueueEntry(null, lost, found, score, ViewingStatus.NEW, null);
    }

    public long getIdOrThrow() {
        if (id == null) {
            throw new PetBedException("MatchQueueEntry is not persisted yet", PetBedException.ErrorCode.MATCH_QUEUE_ENTRY_NOT_PERSISTED);
        }
        return id;
    }

    public void markAsViewed(String viewedBy) {
        if (viewedBy == null || viewedBy.isBlank()) {
            throw new PetBedException("ViewedBy is required", PetBedException.ErrorCode.VIEWED_BY_REQUIRED);
        }
        this.viewingStatus = ViewingStatus.VIEWED;
        this.viewedBy = viewedBy;
    }

    public void markAsRejected() {
        this.viewingStatus = ViewingStatus.REJECTED;
    }

    @Override
    public final boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        MatchQueueEntry that = (MatchQueueEntry) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
