package op.edu.ua.petbed.telegram.callback.handler.pet;

import op.edu.ua.petbed.common.dto.PetDTO;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.common.model.PetSex;
import op.edu.ua.petbed.common.model.PetSize;
import op.edu.ua.petbed.common.model.PetStatus;
import op.edu.ua.petbed.common.model.PetType;
import op.edu.ua.petbed.common.model.UserType;
import op.edu.ua.petbed.pet.PetService;
import op.edu.ua.petbed.telegram.auth.UserAuthContext;
import op.edu.ua.petbed.telegram.callback.CallbackData;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.response.KeyboardLayout;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MyPetsCallbackHandlerTest {

    @Mock
    PetService petService;

    @Mock
    CallbackQueryContext context;

    @InjectMocks
    MyPetsCallbackHandler underTest;

    // .handle() -----------------------------------------------------------------

    @Nested
    class Handle {

        @Test
        void handle_offsetIsNull_throwsPetBedException() {
            // given
            UserAuthContext auth = new UserAuthContext(123L, 1L, UserType.REGULAR, "testuser");
            given(context.auth()).willReturn(auth);
            given(context.callbackData()).willReturn(CallbackData.of(CallbackId.MY_PETS, null, null));

            // when/then
            assertThatThrownBy(() -> underTest.handle(context))
                    .isInstanceOf(PetBedException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PetBedException.ErrorCode.INVALID_CALLBACK);
        }

        @Test
        void handle_offsetNotNull_returnsEditMessageWithPets() {
            // given
            int offset = 0;
            Integer messageId = 456;
            Long chatId = 123L;
            UserAuthContext auth = new UserAuthContext(123L, 1L, UserType.REGULAR, "testuser");

            given(context.auth()).willReturn(auth);
            given(context.chatId()).willReturn(chatId);
            given(context.callbackData()).willReturn(CallbackData.of(CallbackId.MY_PETS, null, offset));
            given(context.messageId()).willReturn(messageId);

            PetDTO pet1 = new PetDTO(1L, 1L, "Barsik", PetType.CAT, "abc123", "Persian", "White", "Solid", 3, PetSex.MALE, PetSize.SMALL, "None", PetStatus.DEFAULT);
            PetDTO pet2 = new PetDTO(2L, 1L, "Murzik", PetType.CAT, "def456", "Siamese", "Cream", "Point", 5, PetSex.FEMALE, PetSize.SMALL, "None", PetStatus.DEFAULT);
            Page<PetDTO> page = new PageImpl<>(List.of(pet1, pet2), PageRequest.of(offset, KeyboardLayout.DEFAULT.pageSize()), 2);
            given(petService.findAllByOwnerId(1L, PageRequest.of(offset, KeyboardLayout.DEFAULT.pageSize()))).willReturn(page);

            // when
            BotApiMethod<?> result = underTest.handle(context);

            // then
            assertThat(result).isInstanceOf(EditMessageText.class);
            EditMessageText message = (EditMessageText) result;
            assertThat(message.getText()).isEqualTo("🐾 Мої улюбленці");
            assertThat(message.getReplyMarkup()).isNotNull();

            verify(petService).findAllByOwnerId(1L, PageRequest.of(offset, KeyboardLayout.DEFAULT.pageSize()));
        }

        @Test
        void handle_emptyPage_returnsEditMessageWithEmptyList() {
            // given
            int offset = 0;
            Integer messageId = 456;
            Long chatId = 123L;
            UserAuthContext auth = new UserAuthContext(123L, 1L, UserType.REGULAR, "testuser");

            given(context.auth()).willReturn(auth);
            given(context.chatId()).willReturn(chatId);
            given(context.callbackData()).willReturn(CallbackData.of(CallbackId.MY_PETS, null, offset));
            given(context.messageId()).willReturn(messageId);

            Page<PetDTO> page = new PageImpl<>(List.of(), PageRequest.of(offset, KeyboardLayout.DEFAULT.pageSize()), 0);
            given(petService.findAllByOwnerId(1L, PageRequest.of(offset, KeyboardLayout.DEFAULT.pageSize()))).willReturn(page);

            // when
            BotApiMethod<?> result = underTest.handle(context);

            // then
            assertThat(result).isInstanceOf(EditMessageText.class);
            EditMessageText message = (EditMessageText) result;
            assertThat(message.getText()).isEqualTo("🐾 Мої улюбленці");
            assertThat(message.getReplyMarkup()).isNotNull();

            verify(petService).findAllByOwnerId(1L, PageRequest.of(offset, KeyboardLayout.DEFAULT.pageSize()));
        }
    }
}