package op.edu.ua.petbed.feed.model;

import jakarta.persistence.*;
import lombok.*;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.common.model.AbstractAuditableEntity;
import op.edu.ua.petbed.feed.dto.CreateFeedPostDTO;
import org.hibernate.proxy.HibernateProxy;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

@NullMarked
@ToString
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, onConstructor_ = @SuppressWarnings("NullAway"))
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "feed_posts")
public class FeedPost extends AbstractAuditableEntity {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "feed_posts_seq")
    @SequenceGenerator(name = "feed_posts_seq", sequenceName = "feed_posts_seq", allocationSize = 50)
    @Column(nullable = false)
    @Getter(AccessLevel.PRIVATE)
    private @Nullable Long id;

    @Column(name = "publisher_id", nullable = false)
    private Long publisherId;

    @Column(columnDefinition = "TEXT")
    private String text;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "location", columnDefinition = "geography(Point, 4326)")
    private Point location;

    public static FeedPost create(CreateFeedPostDTO dto) {
        Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(dto.longitude(), dto.latitude()));
        return new FeedPost(null, dto.publisherId(), dto.text(), dto.photoUrl(), point);
    }

    public long getIdOrThrow() {
        if (id == null) {
            throw new PetBedException("FeedPost is not persisted yet", PetBedException.ErrorCode.FEED_POST_NOT_FOUND);
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
        FeedPost that = (FeedPost) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}