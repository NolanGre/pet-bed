package op.edu.ua.petbed.feed.repository;

import op.edu.ua.petbed.feed.model.FeedPost;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
    FeedPost findNextFeedPost(@Param("userId") Long userId, @Param("userLat") double userLat, @Param("userLon") double userLon);

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

    void deleteAllByPublisherId(Long publisherId);
}