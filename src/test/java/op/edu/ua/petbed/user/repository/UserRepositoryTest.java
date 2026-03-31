package op.edu.ua.petbed.user.repository;

import op.edu.ua.petbed.user.model.User;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Testcontainers
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class UserRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName
            .parse("postgis/postgis:17-3.5-alpine")
            .asCompatibleSubstituteFor("postgres"));

    @Autowired
    UserRepository underTest;

    @Autowired
    TestEntityManager em;

    private User savedUser;

    @BeforeEach
    void setUp() {
        savedUser = underTest.save(User.create(123L, "john_doe"));
        em.flush();
        em.clear();
    }

    // .findByTelegramId() -------------------------------------------------

    @Test
    void findByTelegramId_existing_user_returns_user() {
        // when
        Optional<User> result = underTest.findByTelegramId(savedUser.getTelegramId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getTelegramId()).isEqualTo(savedUser.getTelegramId());
        assertThat(result.get().getTelegramUsername()).isEqualTo(savedUser.getTelegramUsername());
    }

    @Test
    void findByTelegramId_non_existing_user_returns_empty() {
        // when
        Optional<User> result = underTest.findByTelegramId(999L);

        // then
        assertThat(result).isEmpty();
    }

    // .existsByTelegramId() -------------------------------------------------

    @Test
    void existsByTelegramId_existing_user_returns_true() {
        // when
        boolean result = underTest.existsByTelegramId(savedUser.getTelegramId());

        // then
        assertThat(result).isTrue();
    }

    @Test
    void existsByTelegramId_non_existing_user_returns_false() {
        // when
        boolean result = underTest.existsByTelegramId(-1L);

        // then
        assertThat(result).isFalse();
    }

    // .save() -------------------------------------------------

    @Test
    void save_valid_user_persists_with_generated_id() {
        // given
        User user = User.create(456L, "jane_doe");

        // when
        User saved = underTest.save(user);
        em.flush();
        em.clear();

        // then
        assertThat(saved.getIdOrThrow()).isNotNull();
        assertThat(underTest.findById(saved.getIdOrThrow())).isPresent();
    }

    @Test
    void save_duplicate_telegram_id_throws_exception() {
        // given
        User duplicate = User.create(savedUser.getTelegramId(), "another_user");

        // when
        underTest.save(duplicate);

        // then
        assertThatThrownBy(() -> em.flush())
                .isInstanceOfAny(
                        DataIntegrityViolationException.class,
                        ConstraintViolationException.class
                );
    }

    // .equals() / .hashCode() -------------------------------------------------

    @Test
    void equals_same_id_returns_true() {
        // when
        User fetched = underTest.findByTelegramId(savedUser.getTelegramId()).orElseThrow();

        // then
        assertThat(fetched).isEqualTo(savedUser);
    }

    @Test
    void equals_different_id_returns_false() {
        // given
        User another = underTest.save(User.create(456L, "jane_doe"));
        em.flush();
        em.clear();

        // then
        assertThat(another).isNotEqualTo(savedUser);
    }

    @Test
    void equals_null_returns_false() {
        assertThat(savedUser).isNotEqualTo(null);
    }

    @Test
    void hashCode_same_class_returns_same_value() {
        // given
        User another = underTest.save(User.create(456L, "jane_doe"));
        em.flush();
        em.clear();

        // then
        assertThat(savedUser.hashCode()).isEqualTo(another.hashCode());
    }
}