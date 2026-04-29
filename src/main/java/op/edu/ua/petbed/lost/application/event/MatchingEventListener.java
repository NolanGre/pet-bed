package op.edu.ua.petbed.lost.application.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.lost.application.matching.MatchingService;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

/**
 * Event listener for matching-related events.
 * Handles asynchronous matching when lost or found requests are created.
 *
 * <p>Events are processed asynchronously via the MatchingService to avoid
 * blocking the request/response cycle. Users don't wait for matching results;
 * feeds are updated in the background.
 *
 * @see LostRequestCreatedEvent
 * @see FoundRequestCreatedEvent
 * @see MatchingService
 */
@NullMarked
@Component
@RequiredArgsConstructor
@Slf4j
public class MatchingEventListener {

    private final MatchingService matchingService;

    /**
     * Handles LostRequestCreatedEvent by triggering the matching process
     * for the newly created lost request.
     *
     * <p>This finds recent found requests that might match and populates
     * the match_queue for the owner to view later.
     *
     * @param event the event containing the lost request ID
     */
    @EventListener
    public void handleLostRequestCreated(LostRequestCreatedEvent event) {
        log.info("Received LostRequestCreatedEvent for lostRequestId={}", event.lostRequestId());

        try {
            matchingService.processNewLostRequest(event.lostRequestId());
        } catch (DataAccessException e) {
            log.error("Database error processing lost request id={}",
                    event.lostRequestId(), e);
            // Could send to dead letter queue for retry
        } catch (PetBedException e) {
            log.error("Business error processing lost request id={}: {}",
                    event.lostRequestId(), e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error processing lost request id={}",
                    event.lostRequestId(), e);
            // Alert for unexpected errors - this indicates a programming error
        }
    }

    /**
     * Handles FoundRequestCreatedEvent by triggering the matching process
     * for the newly created found request.
     *
     * <p>This finds active lost requests that might match, populates the
     * match_queue for owners, and fills the finder recommendation cache.
     *
     * @param event the event containing the found request ID
     */
    @EventListener
    public void handleFoundRequestCreated(FoundRequestCreatedEvent event) {
        log.info("Received FoundRequestCreatedEvent for foundRequestId={}", event.foundRequestId());

        try {
            matchingService.processNewFoundRequest(event.foundRequestId());
        } catch (DataAccessException e) {
            log.error("Database error processing found request id={}",
                    event.foundRequestId(), e);
            // Could send to dead letter queue for retry
        } catch (PetBedException e) {
            log.error("Business error processing found request id={}: {}",
                    event.foundRequestId(), e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error processing found request id={}",
                    event.foundRequestId(), e);
            // Alert for unexpected errors - this indicates a programming error
        }
    }
}
