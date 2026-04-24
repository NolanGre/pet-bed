package op.edu.ua.petbed.pet.model;

import jakarta.persistence.*;
import lombok.*;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.common.model.AbstractAuditableEntity;
import op.edu.ua.petbed.common.model.PetSex;
import op.edu.ua.petbed.common.model.PetSize;
import op.edu.ua.petbed.common.model.PetStatus;
import op.edu.ua.petbed.common.model.PetType;
import op.edu.ua.petbed.common.dto.CreatePetDTO;
import op.edu.ua.petbed.common.dto.UpdatePetDTO;
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
@Table(name = "pets")
public class Pet extends AbstractAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pets_seq")
    @SequenceGenerator(name = "pets_seq", sequenceName = "pets_seq", allocationSize = 50)
    @Column(nullable = false)
    @Getter(AccessLevel.PRIVATE)
    private @Nullable Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(nullable = false)
    private String name;

    @Column(name = "photo_url", nullable = false)
    private String photoId;

    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    private PetType type;

    @Column(length = 100, nullable = false)
    private String breed;

    @Column(length = 100, nullable = false)
    private String color;

    @Column(name = "color_pattern", length = 100, nullable = false)
    private String colorPattern;

    @Column(nullable = false)
    private Integer age;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private PetSex sex;

    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    private PetSize size;

    @Column(name = "special_marks", columnDefinition = "TEXT", nullable = false)
    private String specialMarks;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PetStatus status = PetStatus.DEFAULT;


    public static Pet create(CreatePetDTO dto) {
        return new Pet(null, dto.ownerId(), dto.name(), dto.photoId(), dto.type(), dto.breed(), dto.color(),
                dto.colorPattern(), dto.age(), dto.sex(), dto.size(), dto.specialMarks(), PetStatus.DEFAULT);
    }

    public long getIdOrThrow() {
        if (id == null) {
            throw new PetBedException("Pet is not persisted yet", PetBedException.ErrorCode.PET_NOT_PERSISTED);
        }
        return id;
    }

    public void update(UpdatePetDTO dto) {
        if (!canUpdate()) {
            throw new PetBedException("Cannot update pet with status: " + status, PetBedException.ErrorCode.PET_CANNOT_UPDATE);
        }
        if (dto.name() != null) this.name = dto.name();
        if (dto.photoId() != null) this.photoId = dto.photoId();
        if (dto.breed() != null) this.breed = dto.breed();
        if (dto.color() != null) this.color = dto.color();
        if (dto.colorPattern() != null) this.colorPattern = dto.colorPattern();
        if (dto.age() != null) this.age = dto.age();
        if (dto.sex() != null) this.sex = dto.sex();
        if (dto.size() != null) this.size = dto.size();
        if (dto.specialMarks() != null) this.specialMarks = dto.specialMarks();
    }

    public boolean canUpdate() {
        return status.canUpdate();
    }

    public boolean canDelete() {
        return status.canDelete();
    }

    @Override
    public final boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Pet pet = (Pet) o;
        return getId() != null && Objects.equals(getId(), pet.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}