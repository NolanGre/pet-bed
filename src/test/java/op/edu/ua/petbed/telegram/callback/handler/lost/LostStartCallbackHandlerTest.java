package op.edu.ua.petbed.telegram.callback.handler.lost;

import op.edu.ua.petbed.common.dto.PetDTO;
import op.edu.ua.petbed.common.model.PetSex;
import op.edu.ua.petbed.common.model.PetSize;
import op.edu.ua.petbed.common.model.PetStatus;
import op.edu.ua.petbed.common.model.PetType;
import op.edu.ua.petbed.pet.PetService;
import op.edu.ua.petbed.telegram.auth.UserAuthContext;
import op.edu.ua.petbed.telegram.callback.CallbackData;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.response.KeyboardLayout;
import op.edu.ua.petbed.telegram.service.TelegramMessageService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LostStartCallbackHandlerTest {

    @Mock
    PetService petService;

    @Mock
    TelegramMessageService telegramMessageService;

    @Mock
    CallbackQueryContext context;

    @Mock
    CallbackData callbackData;

    @Mock
    UserAuthContext auth;

    @InjectMocks
    LostStartCallbackHandler underTest;

    private static final Long USER_ID = 1L;
    private static final Long CHAT_ID = 100L;
    private static final int PAGE_SIZE = KeyboardLayout.DEFAULT.pageSize(); // 5

    @BeforeEach
    void setUp() {
        given(context.callbackData()).willReturn(callbackData);
        given(callbackData.offset()).willReturn(null);
        given(context.auth()).willReturn(auth);
        given(auth.userInternalId()).willReturn(USER_ID);
        given(context.chatId()).willReturn(CHAT_ID);
    }

    @Nested
    class Handle {

        @Test
        void handle_userWithPets_showsPetListWithAddButton() {
            // given
            List<PetDTO> pets = List.of(
                    new PetDTO(1L, USER_ID, "Барсик", PetType.CAT, "photo1", "Дворовий", "Сірий", "", 3, PetSex.MALE, PetSize.MEDIUM, "", PetStatus.DEFAULT),
                    new PetDTO(2L, USER_ID, "Рекс", PetType.DOG, "photo2", "Вівчарка", "Чорний", "", 5, PetSex.MALE, PetSize.LARGE, "", PetStatus.DEFAULT)
            );
            Page<PetDTO> petsPage = new PageImpl<>(pets, PageRequest.of(0, PAGE_SIZE), pets.size());
            given(petService.findPetsAvailableForLostSearch(USER_ID, PageRequest.of(0, PAGE_SIZE))).willReturn(petsPage);

            // when
            underTest.handle(context);

            // then
            ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
            verify(telegramMessageService).editOrSend(any(), captor.capture());

            SendMessage message = captor.getValue();
            InlineKeyboardMarkup replyMarkup = (InlineKeyboardMarkup) message.getReplyMarkup();
            List<InlineKeyboardButton> allButtons = replyMarkup.getKeyboard().stream()
                    .flatMap(Collection::stream)
                    .toList();

            assertThat(allButtons).hasSizeGreaterThanOrEqualTo(4);

            List<String> buttonTexts = allButtons.stream()
                    .map(InlineKeyboardButton::getText)
                    .toList();

            assertThat(buttonTexts).contains("Барсик", "Рекс", "➕ Додати тварину", "⬅️ Повернутись");
        }

        @Test
        void handle_userWithoutPets_showsAddPetOptionOnly() {
            // given
            Page<PetDTO> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, PAGE_SIZE), 0);
            given(petService.findPetsAvailableForLostSearch(USER_ID, PageRequest.of(0, PAGE_SIZE))).willReturn(emptyPage);

            // when
            underTest.handle(context);

            // then
            ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
            verify(telegramMessageService).editOrSend(any(), captor.capture());

            SendMessage message = captor.getValue();
            InlineKeyboardMarkup replyMarkup = (InlineKeyboardMarkup) message.getReplyMarkup();
            List<InlineKeyboardButton> allButtons = replyMarkup.getKeyboard().stream()
                    .flatMap(Collection::stream)
                    .toList();

            assertThat(allButtons).hasSize(2);

            List<String> buttonTexts = allButtons.stream()
                    .map(InlineKeyboardButton::getText)
                    .toList();

            assertThat(buttonTexts)
                    .contains("➕ Додати тварину", "⬅️ Повернутись")
                    .doesNotContain("Барсик", "Рекс");

            assertThat(message.getText()).contains("У вас ще немає доданих тварин");
        }

        @Test
        void handle_displaysCorrectPromptText() {
            // given
            List<PetDTO> pets = List.of(
                    new PetDTO(1L, USER_ID, "Барсик", PetType.CAT, "photo1", "Дворовий", "Сірий", "", 3, PetSex.MALE, PetSize.MEDIUM, "", PetStatus.DEFAULT)
            );
            Page<PetDTO> petsPage = new PageImpl<>(pets, PageRequest.of(0, PAGE_SIZE), 1);
            given(petService.findPetsAvailableForLostSearch(USER_ID, PageRequest.of(0, PAGE_SIZE))).willReturn(petsPage);

            // when
            underTest.handle(context);

            // then
            ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
            verify(telegramMessageService).editOrSend(any(), captor.capture());

            SendMessage message = captor.getValue();
            assertThat(message.getText()).contains("Оберіть тварину для пошуку");
        }
    }
}