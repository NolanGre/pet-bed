package op.edu.ua.petbed.lost.application.event;

import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.lost.application.matching.MatchingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.QueryTimeoutException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MatchingEventListenerTest {

    private static final Long LOST_REQUEST_ID = 100L;
    private static final Long FOUND_REQUEST_ID = 200L;

    @Mock
    private MatchingService matchingService;

    @InjectMocks
    private MatchingEventListener underTest;

    @Nested
    @DisplayName(".handleLostRequestCreated()")
    class HandleLostRequestCreatedTests {

        @Test
        void handleLostRequestCreated_delegatesToMatchingService() {
            // given
            LostRequestCreatedEvent event = new LostRequestCreatedEvent(LOST_REQUEST_ID);
            doNothing().when(matchingService).processNewLostRequest(LOST_REQUEST_ID);

            // when
            underTest.handleLostRequestCreated(event);

            // then
            verify(matchingService).processNewLostRequest(LOST_REQUEST_ID);
        }

        @Test
        void handleLostRequestCreated_withDataAccessException_logsError() {
            // given
            LostRequestCreatedEvent event = new LostRequestCreatedEvent(LOST_REQUEST_ID);
            DataAccessException dbException = new QueryTimeoutException("Database timeout");
            doThrow(dbException).when(matchingService).processNewLostRequest(LOST_REQUEST_ID);

            // when & then - exception should be caught, not propagated
            assertDoesNotThrow(() -> underTest.handleLostRequestCreated(event));
            verify(matchingService).processNewLostRequest(LOST_REQUEST_ID);
        }

        @Test
        void handleLostRequestCreated_withPetBedException_logsError() {
            // given
            LostRequestCreatedEvent event = new LostRequestCreatedEvent(LOST_REQUEST_ID);
            PetBedException businessException = new PetBedException(
                    "Lost request not found",
                    PetBedException.ErrorCode.INTERNAL_ERROR);
            doThrow(businessException).when(matchingService).processNewLostRequest(LOST_REQUEST_ID);

            // when & then - exception should be caught, not propagated
            assertDoesNotThrow(() -> underTest.handleLostRequestCreated(event));
            verify(matchingService).processNewLostRequest(LOST_REQUEST_ID);
        }

        @Test
        void handleLostRequestCreated_withRuntimeException_logsError() {
            // given
            LostRequestCreatedEvent event = new LostRequestCreatedEvent(LOST_REQUEST_ID);
            RuntimeException unexpectedException = new NullPointerException("Unexpected null");
            doThrow(unexpectedException).when(matchingService).processNewLostRequest(LOST_REQUEST_ID);

            // when & then - exception should be caught, not propagated
            assertDoesNotThrow(() -> underTest.handleLostRequestCreated(event));
            verify(matchingService).processNewLostRequest(LOST_REQUEST_ID);
        }
    }

    @Nested
    @DisplayName(".handleFoundRequestCreated()")
    class HandleFoundRequestCreatedTests {

        @Test
        void handleFoundRequestCreated_delegatesToMatchingService() {
            // given
            FoundRequestCreatedEvent event = new FoundRequestCreatedEvent(FOUND_REQUEST_ID);
            doNothing().when(matchingService).processNewFoundRequest(FOUND_REQUEST_ID);

            // when
            underTest.handleFoundRequestCreated(event);

            // then
            verify(matchingService).processNewFoundRequest(FOUND_REQUEST_ID);
        }

        @Test
        void handleFoundRequestCreated_withDataAccessException_logsError() {
            // given
            FoundRequestCreatedEvent event = new FoundRequestCreatedEvent(FOUND_REQUEST_ID);
            DataAccessException dbException = new QueryTimeoutException("Database timeout");
            doThrow(dbException).when(matchingService).processNewFoundRequest(FOUND_REQUEST_ID);

            // when & then - exception should be caught, not propagated
            assertDoesNotThrow(() -> underTest.handleFoundRequestCreated(event));
            verify(matchingService).processNewFoundRequest(FOUND_REQUEST_ID);
        }

        @Test
        void handleFoundRequestCreated_withPetBedException_logsError() {
            // given
            FoundRequestCreatedEvent event = new FoundRequestCreatedEvent(FOUND_REQUEST_ID);
            PetBedException businessException = new PetBedException(
                    "Found request not found",
                    PetBedException.ErrorCode.INTERNAL_ERROR);
            doThrow(businessException).when(matchingService).processNewFoundRequest(FOUND_REQUEST_ID);

            // when & then - exception should be caught, not propagated
            assertDoesNotThrow(() -> underTest.handleFoundRequestCreated(event));
            verify(matchingService).processNewFoundRequest(FOUND_REQUEST_ID);
        }

        @Test
        void handleFoundRequestCreated_withRuntimeException_logsError() {
            // given
            FoundRequestCreatedEvent event = new FoundRequestCreatedEvent(FOUND_REQUEST_ID);
            RuntimeException unexpectedException = new IllegalStateException("Unexpected state");
            doThrow(unexpectedException).when(matchingService).processNewFoundRequest(FOUND_REQUEST_ID);

            // when & then - exception should be caught, not propagated
            assertDoesNotThrow(() -> underTest.handleFoundRequestCreated(event));
            verify(matchingService).processNewFoundRequest(FOUND_REQUEST_ID);
        }
    }
}
