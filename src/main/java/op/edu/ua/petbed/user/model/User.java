package op.edu.ua.petbed.user.model;

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
//@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "users")
public class User extends AbstractAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "users_seq")
    @SequenceGenerator(name = "users_seq", sequenceName = "users_seq", allocationSize = 50)
    @Column(nullable = false)
    @Getter(AccessLevel.PRIVATE)
    private @Nullable Long id;

    @Column(name = "telegram_id", nullable = false, unique = true)
    private Long telegramId;

    @Column(name = "telegram_username", nullable = false)
    private String telegramUsername;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private UserType type = UserType.REGULAR;

    @Column(name = "adoption_history_offset", nullable = false)
    private Integer adoptionHistoryOffset = 0;

    @Column(name = "fostering_history_offset", nullable = false)
    private Integer fosteringHistoryOffset = 0;

    public static User create(Long telegramId, String telegramUsername) {
        if (telegramId == null) {
            throw new PetBedException("telegramId must not be null", PetBedException.ErrorCode.USER_TELEGRAM_ID_REQUIRED);
        }
        if (telegramUsername == null) {
            throw new PetBedException("telegramUsername must not be null", PetBedException.ErrorCode.USER_TELEGRAM_USERNAME_REQUIRED);
        }
        return new User(null, telegramId, telegramUsername, UserType.REGULAR, 0, 0);
    }

    public void switchType() {
        this.type = (this.type == UserType.REGULAR) ? UserType.VOLUNTEER : UserType.REGULAR;
    }

    public long getIdOrThrow() {
        if (id == null) {
            throw new PetBedException("User is not persisted yet", PetBedException.ErrorCode.USER_NOT_PERSISTED);
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
        User user = (User) o;
        return getId() != null && Objects.equals(getId(), user.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }

}