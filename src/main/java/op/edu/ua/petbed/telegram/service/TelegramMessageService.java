package op.edu.ua.petbed.telegram.service;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/**
 * Service for sending messages directly via TelegramClient.
 *
 * <p>Used when handler needs to edit a text message that was previously a media message
 * (e.g., navigating back from PET_DETAIL which shows a photo).
 *
 * <p>Rule: webhook return is preferred for text edits. Only use this service when back/nav
 * action can come from a media screen where edit is not possible.
 */
@Service
@RequiredArgsConstructor
public class TelegramMessageService {

    private final TelegramClient telegramClient;

    public void editOrReplace(Integer messageId, SendMessage message) {
        try {
            telegramClient.execute(EditMessageText.builder()
                    .chatId(message.getChatId())
                    .messageId(messageId)
                    .text(message.getText())
                    .replyMarkup((InlineKeyboardMarkup) message.getReplyMarkup())
                    .build());
        } catch (TelegramApiException e) {
            try {
                telegramClient.execute(message);
                telegramClient.execute(DeleteMessage.builder()
                        .chatId(Long.parseLong(message.getChatId()))
                        .messageId(messageId)
                        .build());
            } catch (TelegramApiException ex) {
                throw new PetBedException("Failed to editOrReplace message", PetBedException.ErrorCode.INTERNAL_ERROR);
            }
        }
    }
}