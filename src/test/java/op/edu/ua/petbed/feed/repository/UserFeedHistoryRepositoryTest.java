package op.edu.ua.petbed.feed.repository;

import op.edu.ua.petbed.feed.model.UserFeedHistory;
import op.edu.ua.petbed.feed.model.UserFeedHistoryId;
import op.edu.ua.petbed.testcontainers.PostgresTestContainer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class UserFeedHistoryRepositoryTest extends PostgresTestContainer {

    @Autowired
    private UserFeedHistoryRepository underTest;

    @Nested
    @DisplayName(".save and .findById")
    class SaveAndFind {

        @Test
        void save_history_entry_can_be_retrieved() {
            // given
            UserFeedHistory history = UserFeedHistory.of(1L, 100L);
            underTest.save(history);

            // when
            Optional<UserFeedHistory> result = underTest.findById(new UserFeedHistoryId(1L, 100L));

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getUserId()).isEqualTo(1L);
            assertThat(result.get().getPostId()).isEqualTo(100L);
        }

        @Test
        void save_multiple_history_entries_retrievable_separately() {
            // given
            underTest.save(UserFeedHistory.of(1L, 100L));
            underTest.save(UserFeedHistory.of(1L, 200L));
            underTest.save(UserFeedHistory.of(2L, 100L));

            // when
            Optional<UserFeedHistory> result1 = underTest.findById(new UserFeedHistoryId(1L, 100L));
            Optional<UserFeedHistory> result2 = underTest.findById(new UserFeedHistoryId(1L, 200L));
            Optional<UserFeedHistory> result3 = underTest.findById(new UserFeedHistoryId(2L, 100L));

            // then
            assertThat(result1).isPresent();
            assertThat(result2).isPresent();
            assertThat(result3).isPresent();
        }
    }

    @Nested
    @DisplayName(".findById")
    class FindById {

        @Test
        void findById_non_existing_returns_empty() {
            // when
            Optional<UserFeedHistory> result = underTest.findById(new UserFeedHistoryId(999L, 999L));

            // then
            assertThat(result).isEmpty();
        }
    }
}