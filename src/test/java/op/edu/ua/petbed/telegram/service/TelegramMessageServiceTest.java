package op.edu.ua.petbed.telegram.service;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class TelegramMessageServiceTest {

    @Mock
    TelegramClient telegramClient;

    @InjectMocks
    TelegramMessageService underTest;

    @Nested
    class EditOrReplace {
        @Test
        void editOrReplace_editSucceeds_noDeleteOrSend() throws Exception {
            Integer messageId = 123;
            SendMessage message = SendMessage.builder()
                    .chatId("456")
                    .text("Hello")
                    .build();

            underTest.editOrReplace(messageId, message);

            verify(telegramClient).execute(any(EditMessageText.class));
            verify(telegramClient, never()).execute(any(DeleteMessage.class));
            verify(telegramClient, never()).execute(message);
        }

        @Test
        void editOrReplace_editFails_deletesAndSends() throws Exception {
            Integer messageId = 123;
            SendMessage message = SendMessage.builder()
                    .chatId("456")
                    .text("Hello")
                    .build();

            doThrow(TelegramApiException.class)
                    .when(telegramClient).execute(any(EditMessageText.class));

            underTest.editOrReplace(messageId, message);

            verify(telegramClient).execute(any(EditMessageText.class));
            verify(telegramClient).execute(any(DeleteMessage.class));
            verify(telegramClient).execute(message);
        }

        @Test
        void editOrReplace_bothEditAndDeleteFail_throwsRuntimeException() throws Exception {
            Integer messageId = 123;
            SendMessage message = SendMessage.builder()
                    .chatId("456")
                    .text("Hello")
                    .build();

            doThrow(TelegramApiException.class)
                    .when(telegramClient).execute(any(EditMessageText.class));

            doThrow(TelegramApiException.class)
                    .when(telegramClient).execute(any(DeleteMessage.class));

            assertThatThrownBy(() -> underTest.editOrReplace(messageId, message))
                    .isInstanceOf(RuntimeException.class);
        }
    }
}
