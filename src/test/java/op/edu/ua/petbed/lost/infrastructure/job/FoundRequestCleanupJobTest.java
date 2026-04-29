package op.edu.ua.petbed.lost.infrastructure.job;

import op.edu.ua.petbed.lost.domain.repository.FoundRequestRepository;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@NullMarked
class FoundRequestCleanupJobTest {

    @Mock
    private FoundRequestRepository foundRequestRepository;

    private FoundRequestCleanupJob underTest;

    @BeforeEach
    void setUp() {
        underTest = new FoundRequestCleanupJob(foundRequestRepository);
    }

    @Nested
    @DisplayName(".cleanupOldFoundRequests()")
    class CleanupOldFoundRequestsTests {

        @Test
        void calls_repository_with_cutoff_date_365_days_ago() {
            // given
            ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
            when(foundRequestRepository.deleteOlderThan(cutoffCaptor.capture())).thenReturn(5);

            Instant beforeExecution = Instant.now();

            // when
            underTest.cleanupOldFoundRequests();

            Instant afterExecution = Instant.now();

            // then
            verify(foundRequestRepository).deleteOlderThan(cutoffCaptor.getValue());

            Instant capturedCutoff = cutoffCaptor.getValue();
            Instant expectedCutoffMin = beforeExecution.minus(Duration.ofDays(365));
            Instant expectedCutoffMax = afterExecution.minus(Duration.ofDays(365));

            assertThat(capturedCutoff)
                .isBetween(expectedCutoffMin, expectedCutoffMax);
        }

        @Test
        void executes_successfully_without_errors() {
            // given
            when(foundRequestRepository.deleteOlderThan(any())).thenReturn(5);

            // when & then - should not throw any exception
            underTest.cleanupOldFoundRequests();

            // verify interaction occurred
            verify(foundRequestRepository).deleteOlderThan(any());
        }

        @Test
        void calculates_cutoff_date_correctly() {
            // given
            ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
            when(foundRequestRepository.deleteOlderThan(cutoffCaptor.capture())).thenReturn(3);

            // when
            underTest.cleanupOldFoundRequests();

            // then
            Instant capturedCutoff = cutoffCaptor.getValue();
            Instant now = Instant.now();
            Duration difference = Duration.between(capturedCutoff, now);

            // Should be approximately 365 days (with small tolerance for test execution time)
            assertThat(difference.toDays()).isEqualTo(365);
        }

        @Test
        void passes_non_null_cutoff_date() {
            // given
            ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
            when(foundRequestRepository.deleteOlderThan(cutoffCaptor.capture())).thenReturn(0);

            // when
            underTest.cleanupOldFoundRequests();

            // then
            assertThat(cutoffCaptor.getValue()).isNotNull();
        }

        @Test
        void handles_repository_exceptions_gracefully() {
            // given
            RuntimeException dbException = new RuntimeException("Database error");
            when(foundRequestRepository.deleteOlderThan(any())).thenThrow(dbException);

            // when - should NOT throw exception (caught internally)
            underTest.cleanupOldFoundRequests();

            // then - repository was called but exception was caught
            verify(foundRequestRepository).deleteOlderThan(any());
        }

        @Test
        void calls_repository_exactly_once() {
            // given
            when(foundRequestRepository.deleteOlderThan(any())).thenReturn(5);

            // when
            underTest.cleanupOldFoundRequests();

            // then
            verify(foundRequestRepository).deleteOlderThan(any());
            verifyNoMoreInteractions(foundRequestRepository);
        }

        @Test
        void handles_zero_deleted_records_gracefully() {
            // given - repository deletes nothing (normal case when no old records exist)
            when(foundRequestRepository.deleteOlderThan(any())).thenReturn(0);

            // when & then - should complete without error
            underTest.cleanupOldFoundRequests();

            verify(foundRequestRepository).deleteOlderThan(any());
        }

        @Test
        void handles_multiple_deleted_records() {
            // given - repository deletes multiple records
            when(foundRequestRepository.deleteOlderThan(any())).thenReturn(42);

            // when
            underTest.cleanupOldFoundRequests();

            // then - should complete without error
            verify(foundRequestRepository).deleteOlderThan(any());
        }
    }

    @Nested
    @DisplayName("Retention period constant")
    class RetentionPeriodTests {

        @Test
        void retention_period_is_365_days() throws Exception {
            // given - access the private static constant via reflection
            java.lang.reflect.Field field = FoundRequestCleanupJob.class.getDeclaredField("RETENTION_PERIOD");
            field.setAccessible(true);
            Duration retentionPeriod = (Duration) field.get(null);

            // then
            assertThat(retentionPeriod).isEqualTo(Duration.ofDays(365));
        }
    }
}
