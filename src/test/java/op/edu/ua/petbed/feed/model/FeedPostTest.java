package op.edu.ua.petbed.feed.model;

import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.feed.dto.CreateFeedPostDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FeedPostTest {

    @Nested
    @DisplayName(".create(CreateFeedPostDTO)")
    class Create {

        @Test
        void create_valid_dto_returns_feed_post() throws Exception {
            // given
            CreateFeedPostDTO dto = new CreateFeedPostDTO(
                    123L,
                    "Test post content",
                    "https://example.com/photo.jpg",
                    50.45,
                    30.52
            );

            // when
            FeedPost result = FeedPost.create(dto);

            // then - use reflection to access private id field
            Field idField = FeedPost.class.getDeclaredField("id");
            idField.setAccessible(true);
            assertThat(idField.get(result)).isNull();
            assertThat(result.getPublisherId()).isEqualTo(123L);
            assertThat(result.getText()).isEqualTo("Test post content");
            assertThat(result.getPhotoUrl()).isEqualTo("https://example.com/photo.jpg");
            assertThat(result.getLocation()).isNotNull();
            assertThat(result.getLocation().getY()).isEqualTo(50.45); // latitude
            assertThat(result.getLocation().getX()).isEqualTo(30.52); // longitude
        }
    }

    @Nested
    @DisplayName(".getIdOrThrow()")
    class GetIdOrThrow {

        @Test
        void getIdOrThrow_when_id_is_null_throws_PetBedException() {
            // given
            CreateFeedPostDTO dto = new CreateFeedPostDTO(
                    123L,
                    "Test content",
                    "photo.jpg",
                    50.0,
                    30.0
            );
            FeedPost feedPost = FeedPost.create(dto);

            // when & then
            assertThatThrownBy(feedPost::getIdOrThrow)
                    .isInstanceOf(PetBedException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PetBedException.ErrorCode.FEED_POST_NOT_FOUND);
        }

        @ParameterizedTest
        @ValueSource(longs = {1L, 100L, Long.MAX_VALUE})
        void getIdOrThrow_when_id_is_set_returns_id(long id) throws Exception {
            // given
            CreateFeedPostDTO dto = new CreateFeedPostDTO(
                    123L,
                    "Test content",
                    "photo.jpg",
                    50.0,
                    30.0
            );
            FeedPost feedPost = FeedPost.create(dto);

            // Set id using reflection (no public setter exists)
            Field idField = FeedPost.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(feedPost, id);

            // when
            long result = feedPost.getIdOrThrow();

            // then
            assertThat(result).isEqualTo(id);
        }
    }
}