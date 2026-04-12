# Testing Rules

## Testing Strategy (TDD)

Follow the Netflix testing recommendations:
- **Unit tests** for Spring beans — use mocks for external dependencies, minimize context
- **Unit tests** for plain Java classes — test behavior directly
- **Integration tests** — use Testcontainers for database-dependent tests

## Test Types

### 1. Unit Tests (Mockito)

For Spring beans (`@Service`, `@Component`, `@Repository`):

```java
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    UserService underTest;

    @Test
    @DisplayName("[Human-readable description]")
    void methodName_scenario_expectedResult() {
        // given (arrange)
        
        // when (act)
        
        // then (assert)
    }
}
```

**Key principles:**
- Mock external dependencies (repositories, other services)
- Minimize Spring context — use `@ExtendWith(MockitoExtension.class)` only
- Test one method at a time
- Use `given()`, `willReturn()` from Mockito BDD style

### 2. Integration Tests (Testcontainers)

For repository tests with real database, use shared configuration via inheritance:

```java
// Extend shared Testcontainers configuration class
import op.edu.ua.petbed.testcontainers.PostgresTestContainer;

@DataJpaTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class UserRepositoryTest extends PostgresTestContainer {

    @Autowired
    UserRepository underTest;

    @BeforeEach
    void setUp() {
        // Use existing data if possible, otherwise create fresh
        // BE CAREFUL: don't let test reuse affect other tests
    }
}
```

**Testcontainers Configuration** (`src/test/java/.../testcontainers/PostgresTestContainer.java`):

```java
package op.edu.ua.petbed.testcontainers;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
public class PostgresTestContainer {

    @Container
    @ServiceConnection
    public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
        DockerImageName.parse("postgis/postgis:17-3.5-alpine")
            .asCompatibleSubstituteFor("postgres")
    );
}
```

**Why inheritance?**
JUnit5 `@Testcontainers` works with `@Container` on **static fields in direct parent class** only. Alternative approaches don't work:
- `@Import(Config.class)` — won't initialize static `@Container`
- Configuration with `@TestConfiguration` — complex lifecycle management

**Key principles:**
- Use `@DataJpaTest` for slice testing
- Extend `PostgresTestContainer` to inherit Testcontainers lifecycle
- **Quality first** — reuse only when it doesn't affect test correctness
- Reuse existing data from previous tests **only if**:
  - The test explicitly needs existing data
  - There's no risk of state leakage
  - Test isolation is preserved
- Clean state between tests when in doubt
- **"Integration"** keyword in class name signals true integration test (not slice tests like `@DataJpaTest`)

### 3. Plain Java Class Tests

For non-Spring classes (DTOs, value objects, utilities):

```java
class UserDTOTest {

    @Test
    void fromEntity_maps_all_fields() {
        // given
        User user = User.create(123L, "username");
        
        // when
        UserDTO dto = UserDTO.fromEntity(user);
        
        // then
        assertThat(dto.telegramId()).isEqualTo(123L);
    }
}
```

## Test Structure

```
src/test/java/op/edu/ua/petbed/
├── testcontainers/              # Shared testcontainers config
│   └── PostgresTestContainer.java
├── user/
│   ├── model/                   # Entity tests (Unit)
│   ├── repository/              # Slice tests (UserRepositoryTest with @DataJpaTest)
│   ├── service/                 # Unit tests (UserServiceTest)
│   └── dto/                     # DTO tests (Unit)
├── common/
│   └── exceptions/             # Exception tests (Unit)
└── telegram/                   # Bot tests
```

**Naming convention:**
- Unit tests: `ClassNameTest` (e.g., `UserServiceTest`)
- Slice tests: `ClassNameTest` (e.g., `UserRepositoryTest` with `@DataJpaTest`)
- Integration tests: `ClassNameIntegrationTest` (e.g., full integration with multiple components)

## Test Organization

Group tests by method using comments:

```java
// .methodName() -------------------------------------------------

@Test
void methodName_scenario_expectedResult() {
    // test implementation
}
```

## Test Naming Convention

```
methodName_scenario_expectedResult()
```

Examples:
- `registerOrGet_existing_user_returns_dto()`
- `registerOrGet_null_telegramId_throws_PetBedException()`
- `findByTelegramId_existing_user_returns_user()`

## Assertions

Use AssertJ for readable assertions:

```java
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Good
assertThat(result.telegramId()).isEqualTo(123L);
assertThat(result).isPresent();

// Exception testing
assertThatThrownBy(() -> underTest.registerOrGet(null, "name"))
    .isInstanceOf(PetBedException.class)
    .hasFieldOrPropertyWithValue("errorCode", PetBedException.ErrorCode.USER_TELEGRAM_ID_REQUIRED);
```

## Mock Verification

```java
// Verify interaction
verify(userRepository).findByTelegramId(123L);
verify(userRepository, never()).save(any());
verifyNoInteractions(userRepository);
```

## Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "op.edu.ua.petbed.user.service.UserServiceTest"

# Run tests with logging (pipe to file to avoid context overflow)
./gradlew test > test.log 2>&1
```

## Model Testing Policy

**DO NOT** test basic JPA entity functionality:
- Basic getters/setters from Lombok
- Standard relationships (`@OneToMany`, `@ManyToOne`, etc.)
- Simple CRUD operations

**DO test**:
- Custom business methods (`User.switchType()`)
- Factory method validation (`User.create()`)
- Complex validation rules

## Test Coverage

Aim for high coverage on:
- Service layer (business logic)
- Domain entities (business methods)
- Exception handling
- Edge cases

## Best Practices

1. **One assertion per test** — or few related assertions
2. **Test edge cases** — null, empty, boundary values
3. **Test happy path** — normal operation
4. **Test error paths** — exceptions, invalid input
5. **Keep tests isolated** — no dependencies between tests
6. **Use descriptive names** — explain what is tested
