package op.edu.ua.petbed.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.common.exceptions.PetBedConfigurationException;
import op.edu.ua.petbed.telegram.UpdateHandlerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.updates.DeleteWebhook;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.telegram.telegrambots.webhook.starter.SpringTelegramWebhookBot;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class TelegramBotConfig {

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.path}")
    private String botPath;

    @Value("${telegram.bot.webhook-url}")
    private String webhookUrl;

    private final UpdateHandlerService updateHandlerService;

    @Bean
    public TelegramClient telegramClient() {
        return new OkHttpTelegramClient(botToken);
    }

    @Bean
    public SpringTelegramWebhookBot petBedBot(TelegramClient telegramClient) {
        SetWebhook setWebhook = SetWebhook.builder()
                .url(webhookUrl + "/" + botPath)
                .build();

        return SpringTelegramWebhookBot.builder()
                .botPath(botPath)
                .updateHandler(updateHandlerService::handle)
                .setWebhook(registerWebhook(telegramClient, setWebhook))
                .deleteWebhook(deleteWebhook(telegramClient))
                .build();
    }

    private Runnable registerWebhook(TelegramClient telegramClient, SetWebhook setWebhook) {
        return () -> {
            try {
                telegramClient.execute(setWebhook);
                log.info("Webhook registered successfully at: {}", setWebhook.getUrl());
            } catch (TelegramApiException e) {
                throw new PetBedConfigurationException("Failed to register Telegram webhook", e);
            }
        };
    }

    private Runnable deleteWebhook(TelegramClient telegramClient) {
        return () -> {
            try {
                telegramClient.execute(new DeleteWebhook());
                log.info("Webhook deleted successfully");
            } catch (TelegramApiException e) {
                throw new PetBedConfigurationException("Failed to delete Telegram webhook", e);
            }
        };
    }
}
