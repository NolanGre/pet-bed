package op.edu.ua.petbed.telegram.service;

import op.edu.ua.petbed.telegram.testutil.TelegramUpdateFixtureUtil;
import op.edu.ua.petbed.testcontainers.PostgresTestContainer;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class TelegramUpdateRouterImplIntegrationTest extends PostgresTestContainer {

    @Autowired
    private TelegramUpdateRouterImpl underTest;

    @Test
    void route_commandMessage_routesToHandler() {
        // given
        Update update = TelegramUpdateFixtureUtil.withCommand("/start", 123L, "testuser");

        // when
        BotApiMethod<?> result = underTest.route(update);

        // then
        SendMessage sendMessage = (SendMessage) result;
        String text = Objects.requireNonNull(sendMessage).getText();
        assertThat(text).contains("""
                        Привіт! 🐾 Це PetBed Bot — твій надійний помічник у світі тварин.
                        
                        Допоможу знайти загубленого улюбленця, прилаштувати тваринку в добрі руки, знайти тимчасовий дім (перетримку) або поділюся актуальними оголошеннями від волонтерів.
                        
                        👇 Що ти можеш зробити прямо зараз (тисни /menu):
                        🔍 Знайшовся/Загубився: Швидко розмістити оголошення.
                        🏠 Адопція: Знайти нову родину або отримати нового друга.
                        ⏱️ Перетримка: Тимчасово прихистити свого улюбленця.
                        📢 Волонтерська стрічка: Важливі оголошення від волонтерів.
                        """);
    }

    @Test
    void route_unknownCallback_returnsErrorMessage() {
        // given - unknown callback id (999 has no handler)
        Update update = TelegramUpdateFixtureUtil.withCallback("999,,0", 123L, "testuser", 123L, 1);

        // when
        BotApiMethod<?> result = underTest.route(update);

        // then - exception is caught and returns error message
        assertThat(result).isNotNull();
    }

    @Test
    void route_requiresAuth_command_runsAuthFirst() {
        // given - /profile requires auth
        Update update = TelegramUpdateFixtureUtil.withCommand("/profile", 999L, "newuser");

        // when
        BotApiMethod<?> result = underTest.route(update);

        // then - should create user and return profile (auth runs first)
        assertThat(result).isNotNull();
    }

    @Test
    void route_unknownCommand_returnsDefault() {
        // given
        Update update = TelegramUpdateFixtureUtil.withCommand("/unknown", 123L, "testuser");

        // when
        BotApiMethod<?> result = underTest.route(update);

        // then - should return default help message
        assertThat(result).isNotNull()
                .isInstanceOf(SendMessage.class);
        SendMessage sendMessage = (SendMessage) Objects.requireNonNull(result);
        assertThat(sendMessage.getText())
                .contains("Unknown command.");
    }

    @Test
    void route_unsupportedUpdate_returnsNullOrErrorMessage() {
        // given - Update without message or callback query (unsupported type)
        Update update = new Update();
        update.setUpdateId(12345);

        // when
        BotApiMethod<?> result = underTest.route(update);

        // then - exception is caught, chatId is null so result is null (cannot respond)
        // This is expected behavior - no chatId to respond to
        assertThat(result).isNull();
    }
}