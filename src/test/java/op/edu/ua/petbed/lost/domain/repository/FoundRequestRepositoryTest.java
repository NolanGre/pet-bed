package op.edu.ua.petbed.lost.domain.repository;

import op.edu.ua.petbed.common.model.PetType;
import op.edu.ua.petbed.lost.domain.model.FoundRequest;
import op.edu.ua.petbed.testcontainers.PostgresTestContainer;
import op.edu.ua.petbed.user.model.User;
import op.edu.ua.petbed.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Coordinate;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FoundRequestRepositoryTest extends PostgresTestContainer {

    @Autowired
    FoundRequestRepository underTest;

    @Autowired
    UserRepository userRepository;

    @Autowired
    TestEntityManager em;

    private Long finderId;
    private FoundRequest savedFoundRequest;

    private static final double BASE_LAT = 50.0;
    private static final double BASE_LON = 30.0;

    @BeforeEach
    void setUp() {
        User user = userRepository.save(createUser(100L, "finder_user"));
        em.flush();
        em.clear();
        finderId = user.getIdOrThrow();

        savedFoundRequest = underTest.save(createFoundRequest(user, PetType.DOG, BASE_LAT, BASE_LON));
        em.flush();
        em.clear();
    }

    private User createUser(Long telegramId, String username) {
        return User.create(telegramId, username);
    }

    private FoundRequest createFoundRequest(User finder, PetType type, double lat, double lon) {
        GeometryFactory gf = new GeometryFactory();
        Point location = gf.createPoint(new Coordinate(lon, lat));
        return FoundRequest.create(finder, "photo_url", type, location, "Test description");
    }

    private Timestamp hoursAgo(int hours) {
        return Timestamp.from(Instant.now().minus(Duration.ofHours(hours)));
    }

    // .findByFinderId -------------------------------------------------------

    @Nested
    class FindByFinderId {

        @Test
        void finder_with_requests_returns_list() {
            List<FoundRequest> result = underTest.findByFinderId(finderId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getIdOrThrow()).isEqualTo(savedFoundRequest.getIdOrThrow());
        }

        @Test
        void finder_without_requests_returns_empty_list() {
            User newUser = userRepository.save(createUser(200L, "new_user"));
            em.flush();
            em.clear();

            List<FoundRequest> result = underTest.findByFinderId(newUser.getIdOrThrow());

            assertThat(result).isEmpty();
        }

        @Test
        void different_finder_returns_empty() {
            User differentUser = userRepository.save(createUser(300L, "different_user"));
            em.flush();
            em.clear();

            List<FoundRequest> result = underTest.findByFinderId(differentUser.getIdOrThrow());

            assertThat(result).isEmpty();
        }
    }

    // .findRecentByPetTypeAndLocation ----------------------------------------

    @Nested
    class FindRecentByPetTypeAndLocation {

        @Test
        void same_type_within_radius_returns_posts() {
            User user = userRepository.findById(finderId).orElseThrow();
            FoundRequest close = underTest.save(createFoundRequest(user, PetType.DOG, 50.01, 30.01)); // ~1.4km
            em.flush();
            em.clear();

            GeometryFactory gf = new GeometryFactory();
            Point baseLocation = gf.createPoint(new Coordinate(BASE_LON, BASE_LAT));

            List<FoundRequest> result = underTest.findRecentByPetTypeAndLocation(
                    PetType.DOG.name(), baseLocation, hoursAgo(24));

            assertThat(result).hasSize(2);
            assertThat(result.stream().map(FoundRequest::getIdOrThrow))
                    .contains(savedFoundRequest.getIdOrThrow(), close.getIdOrThrow());
        }

        @Test
        void different_type_excluded() {
            User user = userRepository.findById(finderId).orElseThrow();
            underTest.save(createFoundRequest(user, PetType.CAT, BASE_LAT, BASE_LON));
            em.flush();
            em.clear();

            GeometryFactory gf = new GeometryFactory();
            Point baseLocation = gf.createPoint(new Coordinate(BASE_LON, BASE_LAT));

            List<FoundRequest> result = underTest.findRecentByPetTypeAndLocation(
                    PetType.DOG.name(), baseLocation, hoursAgo(24));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getPetType()).isEqualTo(PetType.DOG);
        }

        @Test
        void outside_radius_excluded() {
            User user = userRepository.findById(finderId).orElseThrow();
            FoundRequest far = underTest.save(createFoundRequest(user, PetType.DOG, 51.0, 31.0)); // >50km
            em.flush();
            em.clear();

            GeometryFactory gf = new GeometryFactory();
            Point baseLocation = gf.createPoint(new Coordinate(BASE_LON, BASE_LAT));

            List<FoundRequest> result = underTest.findRecentByPetTypeAndLocation(
                    PetType.DOG.name(), baseLocation, hoursAgo(24));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getIdOrThrow()).isEqualTo(savedFoundRequest.getIdOrThrow());
        }

        @Test
        void old_posts_excluded() {
            GeometryFactory gf = new GeometryFactory();
            Point baseLocation = gf.createPoint(new Coordinate(BASE_LON, BASE_LAT));

            // Query with "since" set to 1 second in the future - should exclude all posts
            List<FoundRequest> result = underTest.findRecentByPetTypeAndLocation(
                    PetType.DOG.name(), baseLocation, Timestamp.from(Instant.now().plus(Duration.ofSeconds(1))));

            assertThat(result).isEmpty();
        }

        @Test
        void ordered_by_created_at_desc() {
            User user = userRepository.findById(finderId).orElseThrow();
            FoundRequest newer = underTest.save(createFoundRequest(user, PetType.DOG, 50.01, 30.01));
            em.flush();
            em.clear();

            GeometryFactory gf = new GeometryFactory();
            Point baseLocation = gf.createPoint(new Coordinate(BASE_LON, BASE_LAT));

            List<FoundRequest> result = underTest.findRecentByPetTypeAndLocation(
                    PetType.DOG.name(), baseLocation, hoursAgo(24));

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getIdOrThrow()).isEqualTo(newer.getIdOrThrow());
            assertThat(result.get(1).getIdOrThrow()).isEqualTo(savedFoundRequest.getIdOrThrow());
        }
    }

    // .deleteOlderThan -------------------------------------------------------

    @Nested
    class DeleteOlderThan {

        @Test
        void deletes_old_requests() {
            User user = userRepository.findById(finderId).orElseThrow();
            FoundRequest additional = underTest.save(createFoundRequest(user, PetType.DOG, BASE_LAT, BASE_LON));
            em.flush();
            em.clear();

            // First verify both exist
            assertThat(underTest.findById(additional.getIdOrThrow())).isPresent();
            assertThat(underTest.findById(savedFoundRequest.getIdOrThrow())).isPresent();

            // Delete requests older than 1 second in the future - should delete all
            underTest.deleteOlderThan(Instant.now().plus(Duration.ofSeconds(1)));
            em.flush();
            em.clear();

            // All should be deleted
            assertThat(underTest.findById(additional.getIdOrThrow())).isEmpty();
            assertThat(underTest.findById(savedFoundRequest.getIdOrThrow())).isEmpty();
        }

        @Test
        void keeps_recent_requests() {
            // Delete requests older than 1 hour ago - should remain
            underTest.deleteOlderThan(Instant.now().minus(Duration.ofHours(1)));
            em.flush();
            em.clear();

            assertThat(underTest.findById(savedFoundRequest.getIdOrThrow())).isPresent();
        }
    }
}
