# Feed Module — Implementation Plan

## Overview

План реалізації модуля Feed з детальними технічними рішеннями.

---

## Business Logic Rules (from Analysis)

### R-1 to R-16

Реалізувати відповідно до `feed-module-analysis.md`:
- R-1: Тільки волонтер може додавати пости
- R-2: Кнопка "Створити пост" тільки для волонтерів
- R-3: Кнопка "Мої пости" тільки для волонтерів з >1 постом
- R-4: При зміні типу на REGULAR — пости видаляються
- R-5: Пост містить фото + текст + відстань
- R-6: Відстань < 1 км — в метрах
- R-7: Відстань >= 1 км — в кілометрах
- R-8: Без геолокації — відстань не показується
- R-9: Кнопки "Наступний", "Повернутись"
- R-10: При натисканні — прибирається клавіатура
- R-11: "Повернутись" → до меню Постів
- R-12: "Наступний" → наступний пост
- R-13: Сортування за відстанню
- R-14: Сортування за датою
- R-15: Перегляд → в історію
- R-16: Пропускати переглянуті

---

## 1. Database Migration

### 1.1 Add user location column

```xml
<!-- src/main/resources/db/changelog/0006-add-user-location.xml -->
<changeSet id="0006-01-add-user-location" author="Nolan">
    <sql>ALTER TABLE users ADD COLUMN location GEOGRAPHY(POINT, 4326);</sql>
    <sql>CREATE INDEX idx_users_location ON users USING GIST (location);</sql>
</changeSet>
```

---

## 2. User Module Extension

### 2.1 LocationDTO

```java
// src/main/java/op/edu/ua/petbed/common/dto/LocationDTO.java
@NullMarked
public record LocationDTO(
        double latitude,
        double longitude
) {}
```

### 2.2 User entity

```java
// In User.java
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

@Entity
public class User extends AbstractAuditableEntity {
    
    private static final GeometryFactory geometryFactory = new GeometryFactory();

    @Column(name = "location", columnDefinition = "geography(Point, 4326)")
    private Point location;

    public void setLocation(double latitude, double longitude) {
        this.location = geometryFactory.createPoint(new Coordinate(longitude, latitude));
    }

    public @Nullable Point getLocation() {
        return location;
    }
}
```

### 2.3 UserService extension

```java
// In UserService.java
void setLocation(Long userId, double latitude, double longitude);

@Nullable
LocationDTO getLocation(Long userId);
```

### 2.4 UserServiceImpl extension

```java
// In UserServiceImpl.java
@Override
public void setLocation(Long userId, double latitude, double longitude) {
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new PetBedException("User not found", PetBedException.ErrorCode.USER_NOT_FOUND));
    user.setLocation(latitude, longitude);
    userRepository.save(user);
}

@Override
@Nullable
public LocationDTO getLocation(Long userId) {
    return userRepository.findById(userId)
            .flatMap(user -> user.getLocation() != null 
                ? new LocationDTO(user.getLocation().getY(), user.getLocation().getX())
                : null);
}
```

---

## 3. Feed Module

### 3.1 Package structure

```
src/main/java/op/edu/ua/petbed/feed/
├── FeedService.java
├── service/
│   └── FeedServiceImpl
── model/
│   ├── FeedPost.java
│   └── UserFeedHistory.java
├── repository/
│   ├── FeedPostRepository.java
│   └── UserFeedHistoryRepository.java
└── dto/
    ├── CreateFeedPostDTO.java
    └── FeedPostDTO.java
```

### 3.2 CreateFeedPostDTO

```java
@NullMarked
public record CreateFeedPostDTO(
        Long publisherId,
        String text,
        String photoUrl,
        double latitude,
        double longitude
) {}
```

### 3.3 FeedPostDTO

```java
@NullMarked
public record FeedPostDTO(
        Long id,
        Long publisherId,
        String publisherUsername,
        String text,
        String photoUrl,
        double latitude,
        double longitude,
        @Nullable Double distance,  // In meters, appear only when user with location wanna get post
        Instant createdAt
) {}
```

### 3.4 FeedPost entity

```java
@NullMarked
@Entity
@Table(name = "feed_posts")
public class FeedPost extends AbstractAuditableEntity {

    private static final GeometryFactory geometryFactory = new GeometryFactory();

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

    @Column(columnDefinition = "geography(Point, 4326)")
    private Point location;

    public static FeedPost create(CreateFeedPostDTO dto) {
        return new FeedPost(
                null,
                dto.publisherId(),
                dto.text(),
                dto.photoUrl(),
                geometryFactory.createPoint(
                    new Coordinate(dto.longitude(), dto.latitude())
                )
        );
    }

    public long getIdOrThrow() {
        if (id == null) throw new PetBedException("...");
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
```

### 3.5 UserFeedHistory — сутність для збереження історії переглядів

```java
@NullMarked
@Entity
@Table(name = "users_feed_history")
@IdClass(UserFeedHistoryId.class)
public class UserFeedHistory {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "post_id")
    private Long postId;

    public static UserFeedHistory of(Long userId, Long postId) {
        return new UserFeedHistory(userId, postId);
    }

    @Override
    public final boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        UserFeedHistory that = (UserFeedHistory) o;
        return Objects.equals(userId, that.userId) && Objects.equals(postId, that.postId);
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}

@NullMarked
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserFeedHistoryId implements Serializable {
    private Long userId;
    private Long postId;
}
```

### 3.6 FeedPostRepository

```java
@NullMarked
public interface FeedPostRepository extends JpaRepository<FeedPost, Long> {

    @Query(value = """
        SELECT fp.* FROM feed_posts fp
        WHERE fp.id NOT IN (
            SELECT post_id FROM users_feed_history WHERE user_id = :userId
        )
        ORDER BY
            ST_Distance(
                ST_MakePoint(:userLon, :userLat)::geography,
                fp.location::geography
            ) ASC
        LIMIT 1
        """, nativeQuery = true)
    FeedPost findNextFeedPost(
        @Param("userId") Long userId,
        @Param("userLat") double userLat,
        @Param("userLon") double userLon
    );

    @Query(value = """
        SELECT fp.* FROM feed_posts fp
        WHERE fp.id NOT IN (
            SELECT post_id FROM users_feed_history WHERE user_id = :userId
        )
        ORDER BY fp.created_at DESC
        LIMIT 1
        """, nativeQuery = true)
    FeedPost findNextFeedPostByDate(@Param("userId") Long userId);

    Page<FeedPost> findByPublisherIdOrderByCreatedAtDesc(Long publisherId, Pageable pageable);
}
```

### 3.7 UserFeedHistoryRepository

```java
@NullMarked
public interface UserFeedHistoryRepository extends JpaRepository<UserFeedHistory, UserFeedHistoryId> {
}
```

### 3.8 FeedService

```java
@NullMarked
public interface FeedService {

    FeedPostDTO create(CreateFeedPostDTO dto);

    FeedPostDTO findById(Long id);

    void delete(Long id, Long userId);

    @Nullable FeedPostDTO findNextPostAndMarkAsViewed(Long userId);

    Page<FeedPostDTO> findMyPosts(Long userId, Pageable pageable);

    void deleteAllByPublisherId(Long publisherId);
}
```

### 3.9 FeedServiceImpl

```java
@NullMarked
@Service
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {
    
    private final FeedPostRepository feedPostRepository;
    private final UserFeedHistoryRepository historyRepository;
    private final UserRepository userRepository;

    @Override
    public FeedPostDTO create(CreateFeedPostDTO dto) {
        FeedPost saved = feedPostRepository.save(FeedPost.create(dto));
        return toDto(saved, null);
    }

    @Override
    public FeedPostDTO findById(Long id) {
        FeedPost post = feedPostRepository.findById(id)
                .orElseThrow(() -> new PetBedException("Post not found", ErrorCode.FEED_POST_NOT_FOUND));
        return toDto(post, null);
    }

    @Override
    public void delete(Long id, Long userId) {
        FeedPost post = feedPostRepository.findById(id)
                .orElseThrow(() -> new PetBedException("Post not found", ErrorCode.FEED_POST_NOT_FOUND));
        if (!post.getPublisherId().equals(userId)) {
            throw new PetBedException("Access denied", ErrorCode.FEED_ACCESS_DENIED);
        }
        feedPostRepository.delete(post);
    }

    @Override
    public @Nullable FeedPostDTO findNextPostAndMarkAsViewed(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        Point userLocation = user.getLocation();

        FeedPost post = (userLocation != null)
                ? feedPostRepository.findNextFeedPost(userId, userLocation.getY(), userLocation.getX())
                : feedPostRepository.findNextFeedPostByDate(userId);

        if (post == null) return null;

        historyRepository.save(UserFeedHistory.of(userId, post.getIdOrThrow()));
        return toDto(post, userLocation);
    }

    @Override
    public Page<FeedPostDTO> findMyPosts(Long userId, Pageable pageable) {
        return feedPostRepository.findByPublisherIdOrderByCreatedAtDesc(userId, pageable)
                .map(post -> toDto(post, null));
    }

    @Override
    public void deleteAllByPublisherId(Long publisherId) {
        feedPostRepository.deleteAllByPublisherId(publisherId);
    }

    private FeedPostDTO toDto(FeedPost post, @Nullable Point userLocation) {
        Double distanceMeters = null;
        if (userLocation != null && post.getLocation() != null) {
            distanceMeters = calculateDistanceMeters(userLocation, post.getLocation());
        }
        User publisher = userRepository.findById(post.getPublisherId()).orElseThrow();
        return new FeedPostDTO(
                post.getIdOrThrow(),
                post.getPublisherId(),
                publisher.getTelegramUsername(),
                post.getText(),
                post.getPhotoUrl(),
                post.getLocation().getY(),
                post.getLocation().getX(),
                distanceMeters,
                post.getCreatedAt()
        );
    }
    
    // Haversine formula
    private double calculateDistanceMeters(Point from, Point to) {
    final int R = 6_371_000;
    double lat1 = Math.toRadians(from.getY());
    double lat2 = Math.toRadians(to.getY());
    double deltaLat = Math.toRadians(to.getY() - from.getY());
    double deltaLon = Math.toRadians(to.getX() - from.getX());
    double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
            + Math.cos(lat1) * Math.cos(lat2)
            * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
    return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}
}
```

## 4. Error Codes

```java
// In PetBedException.ErrorCode
FEED_POST_NOT_FOUND("Публікацію не знайдено"),
FEED_ACCESS_DENIED("Ви не можете видалити цю публікацію"),
```

---

## 5. Implementation Order

1. **Migration** — add `users.location` column
2. **User extension** — LocationDTO + User.setLocation + UserService
3. **Feed entities** — FeedPost + UserFeedHistory
4. **Feed repositories** — with PostGIS queries
5. **Feed service** — CRUD + feed queries
6. **Telegram handlers** — FEED_MENU callbacks

---

## 6. PostGIS Notes

- Point.distance(Point) returns planar distance in DEGREES, not meters — use Haversine formula instead
- Point stores (longitude, latitude) in JTS/PostGIS
- Use `.getX()` for longitude, `.getY()` for latitude
- Hibernate Spatial handles conversion automatically