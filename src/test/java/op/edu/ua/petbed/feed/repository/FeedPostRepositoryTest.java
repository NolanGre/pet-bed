package op.edu.ua.petbed.feed.repository;

import op.edu.ua.petbed.common.dto.CreateFeedPostDTO;
import op.edu.ua.petbed.feed.model.FeedPost;
import op.edu.ua.petbed.feed.model.UserFeedHistory;
import op.edu.ua.petbed.testcontainers.PostgresTestContainer;
import op.edu.ua.petbed.user.model.User;
import op.edu.ua.petbed.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FeedPostRepositoryTest extends PostgresTestContainer {

    @Autowired
    FeedPostRepository underTest;
    @Autowired
    UserRepository userRepository;
    @Autowired
    UserFeedHistoryRepository historyRepository;
    @Autowired
    TestEntityManager em;

    private Long publisherId;
    private FeedPost savedPost;

    private static final double USER_LAT = 50.0;
    private static final double USER_LON = 30.0;

    @BeforeEach
    void setUp() {
        User user = userRepository.save(User.create(100L, "feed_user"));
        em.flush();
        em.clear();
        publisherId = user.getIdOrThrow();

        savedPost = underTest.save(createPost(USER_LAT, USER_LON));
        em.flush();
        em.clear();
    }

    private FeedPost createPost(double lat, double lon) {
        return FeedPost.create(new CreateFeedPostDTO(publisherId, "Test text", "photo_url", lat, lon));
    }

    private void markViewed(Long userId, Long postId) {
        historyRepository.save(UserFeedHistory.of(userId, postId));
        em.flush();
        em.clear();
    }

    // .findByPublisherIdOrderByCreatedAtDesc ----------------------------------

    @Nested
    class FindByPublisherIdOrderByCreatedAtDesc {

        @Test
        void existing_publisher_returns_paged_posts() {
            Page<FeedPost> result = underTest.findByPublisherIdOrderByCreatedAtDesc(
                    publisherId, PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getIdOrThrow())
                    .isEqualTo(savedPost.getIdOrThrow());
        }

        @Test
        void no_posts_returns_empty_page() {
            Page<FeedPost> result = underTest.findByPublisherIdOrderByCreatedAtDesc(
                    999L, PageRequest.of(0, 10));

            assertThat(result.getContent()).isEmpty();
        }

        @Test
        void multiple_posts_ordered_newest_first() {
            FeedPost second = underTest.save(createPost(USER_LAT, USER_LON));
            em.flush();
            em.clear();

            Page<FeedPost> result = underTest.findByPublisherIdOrderByCreatedAtDesc(
                    publisherId, PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).getIdOrThrow())
                    .isEqualTo(second.getIdOrThrow());
            assertThat(result.getContent().get(1).getIdOrThrow())
                    .isEqualTo(savedPost.getIdOrThrow());
        }
    }

    // .findNextFeedPost -------------------------------------------------------

    @Nested
    class FindNextFeedPost {

        @Test
        void returns_closest_unviewed_post() {
            FeedPost close = underTest.save(createPost(50.001, 30.001)); // ~140м
            FeedPost far = underTest.save(createPost(50.5, 30.5));   // ~60км
            em.flush();
            em.clear();

            // savedPost (50.0, 30.0) — найближчий до USER_LAT/LON
            FeedPost result = Objects.requireNonNull(underTest.findNextFeedPost(publisherId, USER_LAT, USER_LON));

            assertThat(result.getIdOrThrow()).isEqualTo(savedPost.getIdOrThrow());
        }

        @Test
        void excludes_viewed_posts() {
            markViewed(publisherId, savedPost.getIdOrThrow());

            FeedPost next = underTest.save(createPost(50.1, 30.1));
            em.flush();
            em.clear();

            FeedPost result = Objects.requireNonNull(underTest.findNextFeedPost(publisherId, USER_LAT, USER_LON));

            assertThat(result.getIdOrThrow()).isEqualTo(next.getIdOrThrow());
        }

        @Test
        void returns_null_when_all_posts_viewed() {
            markViewed(publisherId, savedPost.getIdOrThrow());

            FeedPost result = underTest.findNextFeedPost(publisherId, USER_LAT, USER_LON);

            assertThat(result).isNull();
        }

        @Test
        void returns_null_when_no_posts_exist() {
            underTest.deleteAll();
            em.flush();
            em.clear();

            FeedPost result = underTest.findNextFeedPost(publisherId, USER_LAT, USER_LON);

            assertThat(result).isNull();
        }
    }

    // .findNextFeedPostByDate -------------------------------------------------

    @Nested
    class FindNextFeedPostByDate {

        @Test
        void returns_newest_unviewed_post() {
            FeedPost newer = underTest.save(createPost(USER_LAT, USER_LON));
            em.flush();
            em.clear();

            FeedPost result = Objects.requireNonNull(underTest.findNextFeedPostByDate(publisherId));

            assertThat(result.getIdOrThrow()).isEqualTo(newer.getIdOrThrow());
        }

        @Test
        void excludes_viewed_posts() {
            FeedPost newer = underTest.save(createPost(USER_LAT, USER_LON));
            em.flush();
            em.clear();

            markViewed(publisherId, newer.getIdOrThrow());

            FeedPost result = Objects.requireNonNull(underTest.findNextFeedPostByDate(publisherId));

            assertThat(result.getIdOrThrow()).isEqualTo(savedPost.getIdOrThrow());
        }

        @Test
        void returns_null_when_all_posts_viewed() {
            markViewed(publisherId, savedPost.getIdOrThrow());

            FeedPost result = underTest.findNextFeedPostByDate(publisherId);

            assertThat(result).isNull();
        }

        @Test
        void returns_null_when_no_posts_exist() {
            underTest.deleteAll();
            em.flush();
            em.clear();

            FeedPost result = underTest.findNextFeedPostByDate(publisherId);

            assertThat(result).isNull();
        }
    }
}
