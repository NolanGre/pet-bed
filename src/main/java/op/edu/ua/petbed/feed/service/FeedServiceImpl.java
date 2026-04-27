package op.edu.ua.petbed.feed.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.common.dto.UserDTO;
import op.edu.ua.petbed.user.UserService;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.feed.FeedService;
import op.edu.ua.petbed.feed.dto.CreateFeedPostDTO;
import op.edu.ua.petbed.common.dto.FeedPostDTO;
import op.edu.ua.petbed.feed.model.FeedPost;
import op.edu.ua.petbed.feed.model.UserFeedHistory;
import op.edu.ua.petbed.feed.repository.FeedPostRepository;
import op.edu.ua.petbed.feed.repository.UserFeedHistoryRepository;

import java.util.Objects;

@NullMarked
@Service
@RequiredArgsConstructor
@Slf4j
public class FeedServiceImpl implements FeedService {

    private static final int EARTH_RADIUS_METERS = 6_371_000;

    private final FeedPostRepository feedPostRepository;
    private final UserFeedHistoryRepository historyRepository;
    private final UserService userRepository;

    @Override
    public FeedPostDTO create(CreateFeedPostDTO dto) {
        log.debug("Creating feed post for publisher: {}", dto.publisherId());
        FeedPost saved = feedPostRepository.save(FeedPost.create(dto));
        log.info("Created feed post: id={}, publisherId={}", saved.getIdOrThrow(), saved.getPublisherId());
        return toDto(saved, null);
    }

    @Override
    public FeedPostDTO findById(Long id) {
        log.debug("Finding feed post by id: {}", id);
        FeedPost post = feedPostRepository.findById(id)
                .orElseThrow(() -> new PetBedException("Post not found", PetBedException.ErrorCode.FEED_POST_NOT_FOUND));
        return toDto(post, null);
    }

    @Override
    public void delete(Long id, Long userId) {
        log.debug("Deleting feed post: id={}, userId={}", id, userId);
        FeedPost post = feedPostRepository.findById(id)
                .orElseThrow(() -> new PetBedException("Post not found", PetBedException.ErrorCode.FEED_POST_NOT_FOUND));

        if (!post.getPublisherId().equals(userId)) {
            log.warn("Access denied: userId={} attempted to delete post={} owned by publisherId={}",
                    userId, id, post.getPublisherId());
            throw new PetBedException("Access denied", PetBedException.ErrorCode.FEED_ACCESS_DENIED);
        }

        feedPostRepository.delete(post);
        log.info("Deleted feed post: id={}, userId={}", id, userId);
    }

    @Override
    public @Nullable FeedPostDTO findNextPostAndMarkAsViewed(Long userId) {
        log.debug("Finding next feed post for user: {}", userId);
        UserDTO user = userRepository.findById(userId);

        Point userLocation = user.location();
        FeedPost post = (userLocation != null)
                ? feedPostRepository.findNextFeedPost(userId, userLocation.getY(), userLocation.getX())
                : feedPostRepository.findNextFeedPostByDate(userId);

        historyRepository.save(UserFeedHistory.of(userId, post.getIdOrThrow()));
        log.info("Marked post as viewed: postId={}, userId={}", post.getIdOrThrow(), userId);

        return toDto(post, userLocation);
    }

    @Override
    public Page<FeedPostDTO> findMyPosts(Long userId, Pageable pageable) {
        log.debug("Finding feed posts for publisher: {}", userId);
        return feedPostRepository.findByPublisherIdOrderByCreatedAtDesc(userId, pageable)
                .map(post -> toDto(post, null));
    }

    @Override
    public void deleteAllByPublisherId(Long publisherId) {
        log.info("Deleting all feed posts for publisher: {}", publisherId);
        feedPostRepository.deleteAllByPublisherId(publisherId);
        log.info("Deleted all feed posts for publisher: {}", publisherId);
    }

    private FeedPostDTO toDto(FeedPost post, @Nullable Point userLocation) {
        Double distanceMeters = null;
        if (userLocation != null) {
            distanceMeters = calculateDistanceMeters(userLocation, post.getLocation());
        }

        UserDTO publisher = userRepository.findById(post.getPublisherId());

        return new FeedPostDTO(
                post.getIdOrThrow(),
                post.getPublisherId(),
                publisher.telegramUsername(),
                post.getText(),
                post.getPhotoUrl(),
                post.getLocation().getY(),
                post.getLocation().getX(),
                distanceMeters,
                Objects.requireNonNull(post.getCreatedAt())
        );
    }

    private double calculateDistanceMeters(Point from, Point to) {
        double lat1 = Math.toRadians(from.getY());
        double lat2 = Math.toRadians(to.getY());
        double deltaLat = Math.toRadians(to.getY() - from.getY());
        double deltaLon = Math.toRadians(to.getX() - from.getX());

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        return EARTH_RADIUS_METERS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}