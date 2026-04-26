package op.edu.ua.petbed.feed.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class UserFeedHistoryTest {

    @Nested
    @DisplayName(".of(Long, Long)")
    class Of {

        @Test
        void of_creates_entity_with_correct_ids() {
            // given
            Long userId = 123L;
            Long postId = 456L;

            // when
            UserFeedHistory result = UserFeedHistory.of(userId, postId);

            // then
            assertThat(result.getUserId()).isEqualTo(userId);
            assertThat(result.getPostId()).isEqualTo(postId);
        }

        @Test
        void of_with_max_values_returns_correct_ids() {
            // given
            Long userId = Long.MAX_VALUE;
            Long postId = Long.MAX_VALUE;

            // when
            UserFeedHistory result = UserFeedHistory.of(userId, postId);

            // then
            assertThat(result.getUserId()).isEqualTo(userId);
            assertThat(result.getPostId()).isEqualTo(postId);
        }
    }
}