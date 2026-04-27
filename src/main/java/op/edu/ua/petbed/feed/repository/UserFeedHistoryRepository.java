package op.edu.ua.petbed.feed.repository;

import op.edu.ua.petbed.feed.model.UserFeedHistory;
import op.edu.ua.petbed.feed.model.UserFeedHistoryId;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;

@NullMarked
public interface UserFeedHistoryRepository extends JpaRepository<UserFeedHistory, UserFeedHistoryId> {
    void deleteAllByPostId(Long postId);
    void deleteAllByUserId(Long userId);
}