# Testing Rules

## Testing Strategy (TDD)

Follow the Netflix testing recommendations:
- **Unit tests** for Spring beans — use mocks for external dependencies, minimize context
- **Unit tests** for plain Java classes — test behavior directly
- **Integration tests** — use Testcontainers for database-dependent tests

### Test Pyramid

```
E2E / Manual
Integration Tests     ← @SpringBootTest
Slice Tests           ← @DataJpaTest
Unit Tests            ← @ExtendWith(MockitoExtension)
```

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

For full integration tests (`@SpringBootTest`), **always add** `@Transactional` to rollback changes:

```java
@SpringBootTest
@Transactional  // IMPORTANT: rollback after each test
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class TelegramUpdateRouterImplIntegrationTest extends PostgresTestContainer {

    @Autowired
    TelegramUpdateRouter underTest;
}
```

**Why @Transactional?**
- Integration tests modify database (create users, etc.)
- Without `@Transactional`, data persists between test classes
- Next test class sees stale data → constraint violations

**Testcontainers Configuration** (`src/test/java/.../testcontainers/PostgresTestContainer.java`):

```java
package op.edu.ua.petbed.testcontainers;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public abstract class PostgresTestContainer {

    @Container
    @ServiceConnection
    public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
        DockerImageName.parse("postgis/postgis:17-3.5-alpine")
            .asCompatibleSubstituteFor("postgres")
    );
}
```

**Key points:**
- `abstract` class - cannot be instantiated directly
- No `@Testcontainers` annotation - prevents JUnit from stopping container after each test class
- `static` field - shared across all test classes that extend this base
- `@ServiceConnection` - Spring Boot auto-configures datasource

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

## Telegram Bot Testing Utilities

For testing Telegram bot updates (commands, callbacks, messages), **always use** `TelegramUpdateFixtureUtil`:

```java
import op.edu.ua.petbed.telegram.testutil.TelegramUpdateFixtureUtil;

// Command updates
Update update = TelegramUpdateFixtureUtil.withCommand("/start", 123L, "username");
Update update = TelegramUpdateFixtureUtil.withCommand("/start", 123L, "username", 456L);

// Callback updates
Update update = TelegramUpdateFixtureUtil.withCallback("CONFIRM", 123L);
Update update = TelegramUpdateFixtureUtil.withPaginationCallback(10, "item1", 123L);

// Text messages
Update update = TelegramUpdateFixtureUtil.withTextMessage("hello", 123L, 456L);
```

**Location:** `src/test/java/op/edu/ua/petbed/telegram/testutil/TelegramUpdateFixtureUtil.java`

**Why use this utility:**
- Creates real Telegram Update objects (not mocks)
- Follows Telegram Bot API structure exactly
- All tests use consistent fixture format
- Easy to extend for new update types

## Test Structure

```
src/test/java/op/edu/ua/petbed/
├── testcontainers/              # Shared testcontainers config
│   └── PostgresTestContainer.java
├── telegram/
│   └── testutil/               # Test utilities (TelegramUpdateFixtureUtil)
│       └── TelegramUpdateFixtureUtil.java
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

Use `@Nested` classes to group tests by method:

```java
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class InlineKeyboardBuilderTest {

    @Nested
    @DisplayName(".navButtonsFor(CallbackId, Long)")
    class NavButtonsForWithEntityId {

        @Test
        void methodName_scenario_expectedResult() {
            // test implementation
        }
    }
}
```

**Benefits:**
- Better readability - tests grouped by method/feature
- Clear structure - each Nested class has focused scope
- IDE support - easy navigation between test groups

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

## Test Clarity

### Use `underTest` Field Name

Always use `underTest` as the field name for the class being tested:

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    UserService underTest;  // Good - clearly identifies what is being tested

    // ...
}
```

This makes tests more readable and explicitly shows what is being tested.

### Keep Tests Clean

Extract setup, mocks, and context creation into **helper methods** — don't clutter test bodies:

```java
@ExtendWith(MockitoExtension.class)
class PetServiceTest {

    @Mock
    PetRepository petRepository;

    @InjectMocks
    PetService underTest;

    @Test
    void findById_existing_pet_returns_pet() {
        // given
        Pet pet = existingPet();
        given(petRepository.findById(pet.getId())).willReturn(Optional.of(pet));

        // when
        PetDTO result = underTest.findById(pet.getId());

        // then
        assertThat(result.id()).isEqualTo(pet.getId());
    }

    // Helper methods - extract setup to keep tests clean
    private Pet existingPet() {
        return Pet.builder()
            .id(1L)
            .name("Max")
            .build();
    }
}
```

**Benefits:**
- Test body focuses on test logic, not boilerplate
- Reusable setup across tests
- Easier to read and maintain
- Changes to setup only in one place

---

## Common Issues

### Testcontainers conflict: multiple containers started

**Problem:** Each test class starts its own PostgreSQL container.

**Solution:** Use `abstract` base class without `@Testcontainers`:

```java
// Wrong - @Testcontainers stops container after each class
@Testcontainers
public class PostgresTestContainer { }

// Correct - abstract + static field = shared container
public abstract class PostgresTestContainer {
    @Container
    public static PostgreSQLContainer<?> postgres = ...;
}
```

### Test pollution: data persists between test classes

**Problem:** Integration test creates user → next test class sees duplicate.

**Solution:** Add `@Transactional` to integration tests:

```java
@SpringBootTest
@Transactional  // Rolls back after each test
class MyIntegrationTest extends PostgresTestContainer { }
```
## JSpecify + NullAway

NullAway enforces null safety at **compile time** — do not write null-rejection tests for `@NonNull` parameters, they are guaranteed by the compiler.

```java
// ❌ Redundant — NullAway already prevents null being passed here
@Test
void save_null_pet_throws() {
    assertThatThrownBy(() -> underTest.save(null))
        .isInstanceOf(NullPointerException.class);
}
```

> Skip null-path tests unless the parameter is explicitly `@Nullable`.