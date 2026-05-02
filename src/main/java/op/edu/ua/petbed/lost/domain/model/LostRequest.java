package op.edu.ua.petbed.lost.domain.model;

import jakarta.persistence.*;
import lombok.*;
import op.edu.ua.petbed.common.dto.PetDTO;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.common.model.AbstractAuditableEntity;
import op.edu.ua.petbed.common.model.PetType;
import org.hibernate.proxy.HibernateProxy;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.locationtech.jts.geom.Point;

import java.util.Objects;

@NullMarked
@ToString
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, onConstructor_ = @SuppressWarnings("NullAway"))
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "lost_requests")
public class LostRequest extends AbstractAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "lost_requests_seq")
    @SequenceGenerator(name = "lost_requests_seq", sequenceName = "lost_requests_seq", allocationSize = 50)
    @Getter(AccessLevel.PRIVATE)
    private @Nullable Long id;

    @Column(name = "pet_id", nullable = false)
    private Long petId;

    @Column(name = "contact_info", nullable = false)
    private String contactInfo;

    @Column(name = "last_seen_location", nullable = false, columnDefinition = "GEOGRAPHY(POINT, 4326)")
    private Point lastSeenLocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "pet_type", nullable = false)
    private PetType petType;

    @Column(name = "search_text", nullable = false)
    private String searchText;

    public static LostRequest create(PetDTO pet, String contactInfo, Point location) {
        if (contactInfo.isBlank()) {
            throw new PetBedException("Contact info is required to create a lost request", PetBedException.ErrorCode.LOST_REQUEST_INVALID_INPUT);
        }

        String searchText = generateSearchText(pet);

        return new LostRequest(null, pet.id(), contactInfo, location, pet.type(), searchText);
    }

    /**
     * Factory method that accepts pre-normalized search text.
     * Used when text normalization is performed by the service layer.
     */
    public static LostRequest createWithSearchText(PetDTO pet, String contactInfo, Point location, String searchText) {
        if (contactInfo.isBlank()) {
            throw new PetBedException("Contact info is required to create a lost request", PetBedException.ErrorCode.LOST_REQUEST_INVALID_INPUT);
        }
        if (searchText.isBlank()) {
            throw new PetBedException("Search text is required", PetBedException.ErrorCode.LOST_REQUEST_INVALID_INPUT);
        }

        return new LostRequest(null, pet.id(), contactInfo, location, pet.type(), searchText);
    }

    /**
     * Factory method for testing purposes. Allows setting all fields directly.
     */
    public static LostRequest createForTest(Long id, Long petId, String contactInfo, Point location, PetType petType, String searchText) {
        return new LostRequest(id, petId, contactInfo, location, petType, searchText);
    }

    public static String generateSearchText(PetDTO pet) {
        String sb = pet.name() + "; " +
                pet.breed() + "; " +
                pet.color() + "; " +
                pet.colorPattern() + "; " +
                pet.specialMarks();
        return sb.trim();
    }

    public long getIdOrThrow() {
        if (id == null) {
            throw new PetBedException("Lost request is not persisted yet", PetBedException.ErrorCode.LOST_REQUEST_NOT_PERSISTED);
        }
        return id;
    }

    @Override
    public final boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        LostRequest that = (LostRequest) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
