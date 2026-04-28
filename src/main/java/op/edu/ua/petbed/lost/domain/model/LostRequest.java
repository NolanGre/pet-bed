package op.edu.ua.petbed.lost.domain.model;

import jakarta.persistence.*;
import lombok.*;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.common.model.AbstractAuditableEntity;
import op.edu.ua.petbed.common.model.PetType;
import op.edu.ua.petbed.pet.model.Pet;
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
    @Column(nullable = false)
    @Getter(AccessLevel.PRIVATE)
    private @Nullable Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false, unique = true)
    private Pet pet;

    @Column(name = "contact_info", nullable = false)
    private String contactInfo;

    @Column(name = "last_seen_location", nullable = false, columnDefinition = "GEOGRAPHY(POINT, 4326)")
    private Point lastSeenLocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "pet_type", nullable = false)
    private PetType petType;

    @Column(name = "search_text", nullable = false)
    private String searchText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LostRequestStatus status = LostRequestStatus.ACTIVE;

    public static LostRequest create(Pet pet, String contactInfo, Point location) {
        validateNotNull(pet, "Pet");
        validateNotBlank(contactInfo, "Contact info");
        validateNotNull(location, "Location");

        String searchText = generateSearchText(pet);

        return new LostRequest(
                null,
                pet,
                contactInfo,
                location,
                pet.getType(),
                searchText,
                LostRequestStatus.ACTIVE
        );
    }

    private static void validateNotNull(Object value, String fieldName) {
        if (value == null) {
            throw new PetBedException(
                    fieldName + " is required to create a lost request",
                    PetBedException.ErrorCode.LOST_REQUEST_INVALID_INPUT
            );
        }
    }

    private static void validateNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new PetBedException(
                    fieldName + " is required to create a lost request",
                    PetBedException.ErrorCode.LOST_REQUEST_INVALID_INPUT
            );
        }
    }

    private static String generateSearchText(Pet pet) {
        StringBuilder sb = new StringBuilder();
        sb.append(pet.getName()).append(" ");
        sb.append(pet.getBreed()).append(" ");
        sb.append(pet.getColor()).append(" ");
        sb.append(pet.getColorPattern()).append(" ");
        sb.append(pet.getSpecialMarks());
        return sb.toString().trim();
    }

    public long getIdOrThrow() {
        if (id == null) {
            throw new PetBedException(
                    "Lost request is not persisted yet",
                    PetBedException.ErrorCode.LOST_REQUEST_NOT_PERSISTED
            );
        }
        return id;
    }

    public void cancel() {
        this.status = LostRequestStatus.CANCELLED;
    }

    public boolean isActive() {
        return status == LostRequestStatus.ACTIVE;
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
