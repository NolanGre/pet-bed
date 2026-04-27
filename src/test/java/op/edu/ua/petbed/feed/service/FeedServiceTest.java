package op.edu.ua.petbed.feed.service;

import op.edu.ua.petbed.common.dto.UserDTO;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.common.model.UserType;
import op.edu.ua.petbed.feed.dto.CreateFeedPostDTO;
import op.edu.ua.petbed.common.dto.FeedPostDTO;
import op.edu.ua.petbed.feed.model.FeedPost;
import op.edu.ua.petbed.feed.model.UserFeedHistory;
import op.edu.ua.petbed.feed.repository.FeedPostRepository;
import op.edu.ua.petbed.feed.repository.UserFeedHistoryRepository;
import op.edu.ua.petbed.user.UserService;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FeedServiceTest {

    @Mock
    FeedPostRepository feedPostRepository;
    @Mock
    UserFeedHistoryRepository historyRepository;
    @Mock
    UserService userService;

    @InjectMocks
    FeedServiceImpl underTest;

    private static final GeometryFactory GF = new GeometryFactory();

    private static UserDTO userDto(long id, String username, @Nullable Point location) {
        return new UserDTO(id, 100L, username, UserType.VOLUNTEER, location);
    }

    private static FeedPost post(long id, long publisherId, double lat, double lon) {
        FeedPost p = FeedPost.create(new CreateFeedPostDTO(publisherId, "Test text", "photo_url", lat, lon));
        ReflectionTestUtils.setField(p, "id", id);
        ReflectionTestUtils.setField(p, "createdAt", Instant.now());
        return p;
    }

    private static Point point(double lat, double lon) {
        return GF.createPoint(new Coordinate(lon, lat));
    }

    // .create() -----------------------------------------------------------

    @Nested
    class Create {

        @Test
        void valid_dto_returns_feed_post_dto() {
            CreateFeedPostDTO dto = new CreateFeedPostDTO(1L, "Test text", "photo_url", 50.45, 30.52);
            FeedPost saved = post(1L, 1L, 50.45, 30.52);

            given(feedPostRepository.save(any(FeedPost.class))).willReturn(saved);
            given(userService.findById(1L)).willReturn(userDto(1L, "username", null));

            FeedPostDTO result = underTest.create(dto);

            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.publisherId()).isEqualTo(1L);
            assertThat(result.publisherUsername()).isEqualTo("username");
            assertThat(result.text()).isEqualTo("Test text");
            assertThat(result.photoUrl()).isEqualTo("photo_url");
            assertThat(result.distance()).isNull();
            verify(feedPostRepository).save(any(FeedPost.class));
        }
    }

    // .findById() ---------------------------------------------------------

    @Nested
    class FindById {

        @Test
        void existing_post_returns_dto() {
            FeedPost p = post(1L, 1L, 50.45, 30.52);
            given(feedPostRepository.findById(1L)).willReturn(Optional.of(p));
            given(userService.findById(1L)).willReturn(userDto(1L, "username", null));

            FeedPostDTO result = underTest.findById(1L);

            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.text()).isEqualTo("Test text");
            verify(feedPostRepository).findById(1L);
        }

        @Test
        void non_existing_post_throws_PetBedException() {
            given(feedPostRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> underTest.findById(99L))
                    .isInstanceOf(PetBedException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PetBedException.ErrorCode.FEED_POST_NOT_FOUND);
        }
    }

    // .delete() -----------------------------------------------------------

    @Nested
    class Delete {

        @Test
        void owner_can_delete_post() {
            FeedPost p = post(1L, 1L, 50.45, 30.52);
            given(feedPostRepository.findById(1L)).willReturn(Optional.of(p));

            underTest.delete(1L, 1L);

            verify(feedPostRepository).findById(1L);
            verify(feedPostRepository).delete(p);
        }

        @Test
        void non_existing_post_throws_PetBedException() {
            given(feedPostRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> underTest.delete(99L, 1L))
                    .isInstanceOf(PetBedException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PetBedException.ErrorCode.FEED_POST_NOT_FOUND);
        }

        @Test
        void other_users_post_throws_PetBedException() {
            FeedPost p = post(1L, 2L, 50.45, 30.52); // publisherId=2, але видаляє userId=1
            given(feedPostRepository.findById(1L)).willReturn(Optional.of(p));

            assertThatThrownBy(() -> underTest.delete(1L, 1L))
                    .isInstanceOf(PetBedException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PetBedException.ErrorCode.FEED_ACCESS_DENIED);
            verify(feedPostRepository, never()).delete(any());
        }
    }

    // .findNextPostAndMarkAsViewed() --------------------------------------

    @Nested
    class FindNextPostAndMarkAsViewed {

        @Test
        void with_location_returns_post_with_distance_and_saves_history() {
            long userId = 1L;
            Point userLocation = point(50.45, 30.52);
            FeedPost p = post(1L, 2L, 50.46, 30.53);

            given(userService.findById(userId)).willReturn(userDto(userId, "username", userLocation));
            given(userService.findById(2L)).willReturn(userDto(2L, "publisher", null));
            given(feedPostRepository.findNextFeedPost(eq(userId), anyDouble(), anyDouble())).willReturn(p);
            given(historyRepository.save(any())).willReturn(UserFeedHistory.of(userId, 1L));

            FeedPostDTO result = underTest.findNextPostAndMarkAsViewed(userId);

            assertThat(result).isNotNull();
            FeedPostDTO nonNull = Objects.requireNonNull(result);
            assertThat(nonNull.publisherId()).isEqualTo(2L);
            assertThat(result.publisherUsername()).isEqualTo("publisher");
            assertThat(result.distance()).isNotNull();
            verify(historyRepository).save(any());
            verify(feedPostRepository).findNextFeedPost(eq(userId), anyDouble(), anyDouble());
            verify(feedPostRepository, never()).findNextFeedPostByDate(any());
        }

        @Test
        void without_location_returns_post_by_date_without_distance() {
            long userId = 1L;
            FeedPost p = post(1L, 2L, 50.45, 30.52);

            given(userService.findById(userId)).willReturn(userDto(userId, "username", null));
            given(userService.findById(2L)).willReturn(userDto(2L, "publisher", null));
            given(feedPostRepository.findNextFeedPostByDate(userId)).willReturn(p);
            given(historyRepository.save(any())).willReturn(UserFeedHistory.of(userId, 1L));

            FeedPostDTO result = underTest.findNextPostAndMarkAsViewed(userId);

            assertThat(result).isNotNull();
            verify(historyRepository).save(any());
            verify(feedPostRepository).findNextFeedPostByDate(userId);
            verify(feedPostRepository, never()).findNextFeedPost(any(), anyDouble(), anyDouble());
        }
    }

    // .findMyPosts() ------------------------------------------------------

    @Nested
    class FindMyPosts {

        @Test
        void returns_paged_dtos() {
            long userId = 1L;
            Pageable pageable = PageRequest.of(0, 10);
            FeedPost p = post(1L, userId, 50.45, 30.52);
            Page<FeedPost> page = new PageImpl<>(List.of(p), pageable, 1);

            given(feedPostRepository.findByPublisherIdOrderByCreatedAtDesc(userId, pageable)).willReturn(page);
            given(userService.findById(userId)).willReturn(userDto(userId, "username", null));

            Page<FeedPostDTO> result = underTest.findMyPosts(userId, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).id()).isEqualTo(1L);
            assertThat(result.getContent().get(0).publisherId()).isEqualTo(userId);
            verify(feedPostRepository).findByPublisherIdOrderByCreatedAtDesc(userId, pageable);
        }

        @Test
        void no_posts_returns_empty_page() {
            long userId = 1L;
            Pageable pageable = PageRequest.of(0, 10);
            Page<FeedPost> emptyPage = Page.empty(pageable);

            given(feedPostRepository.findByPublisherIdOrderByCreatedAtDesc(userId, pageable)).willReturn(emptyPage);

            Page<FeedPostDTO> result = underTest.findMyPosts(userId, pageable);

            assertThat(result.getContent()).isEmpty();
        }
    }

    // .deleteAllByPublisherId() -------------------------------------------

    @Nested
    class DeleteAllByPublisherId {

        @Test
        void deletes_all_posts_for_publisher() {
            underTest.deleteAllByPublisherId(1L);

            verify(feedPostRepository).deleteAllByPublisherId(1L);
        }
    }
}
