package op.edu.ua.petbed.telegram.callback.handler.pet;

import op.edu.ua.petbed.common.dto.PetDTO;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.common.model.*;
import op.edu.ua.petbed.pet.PetService;
import op.edu.ua.petbed.telegram.auth.UserAuthContext;
import op.edu.ua.petbed.telegram.callback.CallbackData;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.response.KeyboardLayout;
import op.edu.ua.petbed.telegram.service.TelegramMessageService;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MyPetsCallbackHandlerTest {

    @Mock
    PetService petService;

    @Mock
    TelegramMessageService telegramMessageService;

    @Mock
    CallbackQueryContext context;

    @InjectMocks
    MyPetsCallbackHandler underTest;

    // .handle() -----------------------------------------------------------------

    @Nested
    class Handle {

        @Test
        void handle_offsetIsNull_throwsIllegalStateException() {
            // given
            given(context.callbackData()).willReturn(CallbackData.of(CallbackId.MY_PETS, null, null));

            // when/then
            assertThatThrownBy(() -> underTest.handle(context))
                    .isInstanceOf(PetBedException.class);
        }

        @Test
        void handle_offsetNotNull_callsTelegramMessageServiceAndReturnsAnswerCallbackQuery() {
            // given
            int offset = 0;
            Integer messageId = 456;
            Long chatId = 123L;
            UserAuthContext auth = new UserAuthContext(123L, 1L, UserType.REGULAR, "testuser");
            CallbackQuery callbackQuery = mock(CallbackQuery.class);
            given(callbackQuery.getId()).willReturn("callbackId123");
            given(context.auth()).willReturn(auth);
            given(context.chatId()).willReturn(chatId);
            given(context.callbackData()).willReturn(CallbackData.of(CallbackId.MY_PETS, null, offset));
            given(context.messageId()).willReturn(messageId);
            given(context.callbackQuery()).willReturn(callbackQuery);

            PetDTO pet1 = new PetDTO(1L, 1L, "Barsik", PetType.CAT, "abc123", "Persian", "White", "Solid", 3, PetSex.MALE, PetSize.SMALL, "None", PetStatus.DEFAULT);
            PetDTO pet2 = new PetDTO(2L, 1L, "Murzik", PetType.CAT, "def456", "Siamese", "Cream", "Point", 5, PetSex.FEMALE, PetSize.SMALL, "None", PetStatus.DEFAULT);
            Page<PetDTO> page = new PageImpl<>(List.of(pet1, pet2), PageRequest.of(offset, KeyboardLayout.DEFAULT.pageSize()), 2);
            given(petService.findAllByOwnerId(1L, PageRequest.of(offset, KeyboardLayout.DEFAULT.pageSize()))).willReturn(page);

            // when
            BotApiMethod<?> result = underTest.handle(context);

            // then
            assertThat(result).isInstanceOf(AnswerCallbackQuery.class);
            verify(telegramMessageService).editOrReplace(eq(messageId), any());
            verify(petService).findAllByOwnerId(1L, PageRequest.of(offset, KeyboardLayout.DEFAULT.pageSize()));
        }

        @Test
        void handle_emptyPage_returnsAnswerCallbackQuery() {
            // given
            int offset = 0;
            Integer messageId = 456;
            Long chatId = 123L;
            UserAuthContext auth = new UserAuthContext(123L, 1L, UserType.REGULAR, "testuser");
            CallbackQuery callbackQuery = mock(CallbackQuery.class);
            given(callbackQuery.getId()).willReturn("callbackId123");
            given(context.auth()).willReturn(auth);
            given(context.chatId()).willReturn(chatId);
            given(context.callbackData()).willReturn(CallbackData.of(CallbackId.MY_PETS, null, offset));
            given(context.messageId()).willReturn(messageId);
            given(context.callbackQuery()).willReturn(callbackQuery);

            Page<PetDTO> page = new PageImpl<>(List.of(), PageRequest.of(offset, KeyboardLayout.DEFAULT.pageSize()), 0);
            given(petService.findAllByOwnerId(1L, PageRequest.of(offset, KeyboardLayout.DEFAULT.pageSize()))).willReturn(page);

            // when
            BotApiMethod<?> result = underTest.handle(context);

            // then
            assertThat(result).isInstanceOf(AnswerCallbackQuery.class);
            verify(telegramMessageService).editOrReplace(eq(messageId), any());
            verify(petService).findAllByOwnerId(1L, PageRequest.of(offset, KeyboardLayout.DEFAULT.pageSize()));
        }
    }
}