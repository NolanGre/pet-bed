package op.edu.ua.petbed.lost.domain.model;

import jakarta.persistence.*;
import lombok.*;
import op.edu.ua.petbed.common.dto.PetDTO;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.common.model.AbstractAuditableEntity;
import op.edu.ua.petbed.common.model.PetType;
import op.edu.ua.petbed.lost.application.dto.CreateLostRequestDTO;
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

    @Column(name = "breed_text")
    private @Nullable String breedText;

    @Column(name = "color_text")
    private @Nullable String colorText;

    @Column(name = "coat_text")
    private @Nullable String coatText;

    @Column(name = "size_text")
    private @Nullable String sizeText;

    @Column(name = "sex_text")
    private @Nullable String sexText;

    @Column(name = "features_text")
    private @Nullable String featuresText;

    public static LostRequest create(PetDTO pet, String contactInfo, Point location) {
        if (contactInfo.isBlank()) {
            throw new PetBedException("Contact info is required to create a lost request", PetBedException.ErrorCode.LOST_REQUEST_INVALID_INPUT);
        }

        CreateLostRequestDTO dto = CreateLostRequestDTO.builder()
                .petId(pet.id())
                .contactInfo(contactInfo)
                .location(location)
                .petType(pet.type())
                .breedText(pet.breed())
                .colorText(pet.color())
                .coatText(pet.colorPattern())
                .sizeText(pet.size() != null ? pet.size().name() : null)
                .sexText(pet.sex() != null ? pet.sex().name() : null)
                .featuresText(pet.specialMarks())
                .build();

        return create(dto);
    }

    public static LostRequest create(CreateLostRequestDTO dto) {
        return new LostRequest(
                null,
                dto.petId(),
                dto.contactInfo(),
                dto.location(),
                dto.petType(),
                dto.breedText(),
                dto.colorText(),
                dto.coatText(),
                dto.sizeText(),
                dto.sexText(),
                dto.featuresText()
        );
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
