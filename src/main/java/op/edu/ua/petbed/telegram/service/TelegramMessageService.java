package op.edu.ua.petbed.telegram.service;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
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

    public AnswerCallbackQuery editOrReplace(CallbackQueryContext context, SendMessage message) {
        try {
            telegramClient.execute(EditMessageText.builder()
                    .chatId(message.getChatId())
                    .messageId(context.messageId())
                    .text(message.getText())
                    .replyMarkup((InlineKeyboardMarkup) message.getReplyMarkup())
                    .build());
        } catch (TelegramApiException e) {
            try {
                telegramClient.execute(message);
                telegramClient.execute(DeleteMessage.builder()
                        .chatId(Long.parseLong(message.getChatId()))
                        .messageId(context.messageId())
                        .build());
            } catch (TelegramApiException ex) {
                throw new PetBedException("Failed to editOrReplace message", PetBedException.ErrorCode.INTERNAL_ERROR);
            }
        }

        return AnswerCallbackQuery.builder()
                .callbackQueryId(context.callbackQuery().getId())
                .build();
    }

    //TODO tests

    /**
     * When we need to keep last message if we can't edit it.
     */
    public AnswerCallbackQuery editOrSend(CallbackQueryContext context, SendMessage message) {
        try {
            telegramClient.execute(EditMessageText.builder()
                    .chatId(message.getChatId())
                    .messageId(context.messageId())
                    .text(message.getText())
                    .replyMarkup((InlineKeyboardMarkup) message.getReplyMarkup())
                    .build());
        } catch (TelegramApiException e) {
            try {
                telegramClient.execute(EditMessageReplyMarkup.builder()
                        .chatId(message.getChatId())
                        .messageId(context.messageId())
                        .replyMarkup(InlineKeyboardMarkup.builder().build())
                        .build());
            } catch (TelegramApiException ignored) {
            }
            try {
                telegramClient.execute(message);
            } catch (TelegramApiException ex) {
                throw new PetBedException("Failed to editOrSend message", PetBedException.ErrorCode.INTERNAL_ERROR);
            }
        }

        return AnswerCallbackQuery.builder()
                .callbackQueryId(context.callbackQuery().getId())
                .build();
    }

    /**
     * Removes the inline keyboard from a message.
     *
     * @param chatId    the chat ID
     * @param messageId the message ID
     */
    public void removeKeyboard(Long chatId, Integer messageId) {
        try {
            telegramClient.execute(EditMessageReplyMarkup.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .replyMarkup(InlineKeyboardMarkup.builder().build())
                    .build());
        } catch (TelegramApiException e) {
            // Ignore - message might be too old or already edited
        }
    }
}