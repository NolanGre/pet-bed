package op.edu.ua.petbed.lost.domain.model;

import jakarta.persistence.*;
import lombok.*;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.common.model.AbstractAuditableEntity;
import op.edu.ua.petbed.common.model.PetType;
import op.edu.ua.petbed.lost.application.dto.CreateFoundRequestDTO;
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
@Table(name = "found_requests")
public class FoundRequest extends AbstractAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "found_requests_seq")
    @SequenceGenerator(name = "found_requests_seq", sequenceName = "found_requests_seq", allocationSize = 50)
    @Column(nullable = false)
    @Getter(AccessLevel.PRIVATE)
    private @Nullable Long id;

    @Column(nullable = false)
    private Long finderId;

    @Column(name = "photo_url", nullable = false)
    private String photoUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "pet_type", nullable = false)
    private PetType petType;

    @Column(nullable = false, columnDefinition = "GEOGRAPHY(POINT, 4326)")
    private Point location;

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

    public static FoundRequest create(CreateFoundRequestDTO dto) {
        if (dto.photoUrl().isBlank()) {
            throw new PetBedException("Photo URL is required", PetBedException.ErrorCode.LOST_REQUEST_PHOTO_URL_REQUIRED);
        }

        return new FoundRequest(
                null,
                dto.finderId(),
                dto.photoUrl(),
                dto.petType(),
                dto.location(),
                dto.breed(),
                dto.color(),
                dto.coat(),
                dto.size(),
                dto.sex(),
                dto.features()
        );
    }

    public long getIdOrThrow() {
        if (id == null) {
            throw new PetBedException("Found request is not persisted yet", PetBedException.ErrorCode.LOST_REQUEST_NOT_PERSISTED);
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
        FoundRequest that = (FoundRequest) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
