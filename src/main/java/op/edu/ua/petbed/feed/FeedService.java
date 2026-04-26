package op.edu.ua.petbed.feed;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import op.edu.ua.petbed.feed.dto.CreateFeedPostDTO;
import op.edu.ua.petbed.feed.dto.FeedPostDTO;

@NullMarked
public interface FeedService {

    FeedPostDTO create(CreateFeedPostDTO dto);

    FeedPostDTO findById(Long id);

    void delete(Long id, Long userId);

    @Nullable
    FeedPostDTO findNextPostAndMarkAsViewed(Long userId);

    Page<FeedPostDTO> findMyPosts(Long userId, Pageable pageable);

    void deleteAllByPublisherId(Long publisherId);
}