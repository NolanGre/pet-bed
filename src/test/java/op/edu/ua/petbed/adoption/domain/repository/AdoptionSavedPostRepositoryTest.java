package op.edu.ua.petbed.adoption.domain.repository;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * Repository tests for AdoptionSavedPostRepository.
 * TODO: Implement tests after fixing existing test failures.
 */
@Disabled("Skipped: Waiting for test environment stabilization")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AdoptionSavedPostRepositoryTest {

    @Test
    @DisplayName("TC-ASP-001: Find by user id returns saved posts for user")
    void findByUserId_existingUser_returnsSavedPosts() {
        // TODO: Implement test
    }

    @Test
    @DisplayName("TC-ASP-002: Exists by post id and user id returns true for saved")
    void existsByPostIdAndUserId_saved_returnsTrue() {
        // TODO: Implement test
    }

    @Test
    @DisplayName("TC-ASP-003: Delete by post id and user id removes saved post")
    void deleteByPostIdAndUserId_existing_removesPost() {
        // TODO: Implement test
    }
}
