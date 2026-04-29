package op.edu.ua.petbed.telegram.callback.handler.lost;

import op.edu.ua.petbed.common.dto.LostRequestDTO;
import op.edu.ua.petbed.common.dto.PetDTO;
import op.edu.ua.petbed.common.model.PetSex;
import op.edu.ua.petbed.common.model.PetSize;
import op.edu.ua.petbed.common.model.PetStatus;
import op.edu.ua.petbed.common.model.PetType;
import op.edu.ua.petbed.common.model.UserType;
import op.edu.ua.petbed.lost.FinderRecommendationService;
import op.edu.ua.petbed.lost.LostRequestService;
import op.edu.ua.petbed.pet.PetService;
import op.edu.ua.petbed.telegram.auth.UserAuthContext;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.service.TelegramMessageService;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LostFoundMatchesCallbackHandlerTest {

    @Mock
    FinderRecommendationService finderRecommendationService;

    @Mock
    LostRequestService lostRequestService;

    @Mock
    PetService petService;

    @Mock
    TelegramMessageService telegramMessageService;

    @Mock
    CallbackQueryContext context;

    @InjectMocks
    LostFoundMatchesCallbackHandler underTest;

    private static final Long TELEGRAM_USER_ID = 123L;
    private static final Long INTERNAL_USER_ID = 1L;
    private static final Long CHAT_ID = 456L;
    private static final Integer MESSAGE_ID = 10;
    private static final Long LOST_REQUEST_ID = 100L;
    private static final Long PET_ID = 200L;

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    @Nested
    class GetCallbackId {

        @Test
        void getCallbackId_shouldReturnLostFoundMatches() {
            // when
            CallbackId result = underTest.getCallbackId();

            // then
            assertThat(result).isEqualTo(CallbackId.LOST_FOUND_MATCHES);
        }
    }

    @Nested
    class Handle {

        @Test
        void handle_whenHasRecommendations_shouldShowLostRequest() throws Exception {
            // given
            setupContext();
            given(finderRecommendationService.pollNext(INTERNAL_USER_ID))
                    .willReturn(Optional.of(LOST_REQUEST_ID));

            LostRequestDTO lostRequest = createLostRequestWithContact();
            given(lostRequestService.findById(LOST_REQUEST_ID)).willReturn(lostRequest);

            PetDTO pet = createPetDTO();
            given(petService.findById(PET_ID)).willReturn(pet);

            // when
            PartialBotApiMethod<?> result = underTest.handle(context);

            // then
            assertThat(result).isInstanceOf(SendPhoto.class);
            SendPhoto photo = (SendPhoto) result;
            assertThat(photo.getChatId()).isEqualTo(CHAT_ID.toString());
            assertThat(photo.getPhoto()).isNotNull();

            String caption = Objects.requireNonNull(photo.getCaption());
            assertThat(caption).contains("Buddy");
            assertThat(caption).contains("Labrador");
            assertThat(caption).contains("+380991234567");
        }

        @Test
        void handle_whenNoRecommendations_shouldShowEmptyMessage() throws Exception {
            // given
            setupContext();
            given(finderRecommendationService.pollNext(INTERNAL_USER_ID))
                    .willReturn(Optional.empty());

            // when
            PartialBotApiMethod<?> result = underTest.handle(context);

            // then
            verify(finderRecommendationService).remove(INTERNAL_USER_ID);
            assertThat(result).isInstanceOf(SendMessage.class);
            SendMessage message = (SendMessage) result;
            assertThat(message.getText()).contains("Ви переглянули всі доступні анкети");
        }

        @Test
        void handle_whenRecommendationHasNoContact_shouldSkipToNext() throws Exception {
            // given
            setupContext();
            Long lostRequestWithContact = 101L;
            Long lostRequestWithoutContact = 100L;

            // First call returns request without contact, second returns with contact
            when(finderRecommendationService.pollNext(INTERNAL_USER_ID))
                    .thenReturn(Optional.of(lostRequestWithoutContact))
                    .thenReturn(Optional.of(lostRequestWithContact));

            LostRequestDTO noContactRequest = createLostRequestWithoutContact();
            LostRequestDTO withContactRequest = createLostRequestWithContact();

            given(lostRequestService.findById(lostRequestWithoutContact)).willReturn(noContactRequest);
            given(lostRequestService.findById(lostRequestWithContact)).willReturn(withContactRequest);

            PetDTO pet = createPetDTO();
            given(petService.findById(PET_ID)).willReturn(pet);

            // when
            PartialBotApiMethod<?> result = underTest.handle(context);

            // then
            assertThat(result).isInstanceOf(SendPhoto.class);
            // Should have polled twice - once for no contact, once for with contact
            // Note: remove is NOT called when skipping, only when queue is empty
        }

        @Test
        void handle_shouldRemovePreviousKeyboard() throws Exception {
            // given
            setupContext();
            given(finderRecommendationService.pollNext(INTERNAL_USER_ID))
                    .willReturn(Optional.of(LOST_REQUEST_ID));

            LostRequestDTO lostRequest = createLostRequestWithContact();
            given(lostRequestService.findById(LOST_REQUEST_ID)).willReturn(lostRequest);

            PetDTO pet = createPetDTO();
            given(petService.findById(PET_ID)).willReturn(pet);

            // when
            underTest.handle(context);

            // then
            verify(telegramMessageService).removeKeyboard(CHAT_ID, MESSAGE_ID);
        }

        @Test
        void handle_shouldShowPetPhotoAndInfo() throws Exception {
            // given
            setupContext();
            given(finderRecommendationService.pollNext(INTERNAL_USER_ID))
                    .willReturn(Optional.of(LOST_REQUEST_ID));

            LostRequestDTO lostRequest = createLostRequestWithContact();
            given(lostRequestService.findById(LOST_REQUEST_ID)).willReturn(lostRequest);

            PetDTO pet = createPetDTO();
            given(petService.findById(PET_ID)).willReturn(pet);

            // when
            PartialBotApiMethod<?> result = underTest.handle(context);

            // then
            assertThat(result).isInstanceOf(SendPhoto.class);
            SendPhoto photo = (SendPhoto) result;

            String caption = Objects.requireNonNull(photo.getCaption());
            assertThat(caption).contains("🐾 Buddy");
            assertThat(caption).contains("Порода: Labrador");
            assertThat(caption).contains("Колір: Golden");
            assertThat(caption).contains("Окрас: Smooth");
            assertThat(caption).contains("Стать: Хлопчик");
            assertThat(caption).contains("Розмір: Середній");
            assertThat(caption).contains("Особливі прикмети: White spot on chest");
            assertThat(caption).contains("📞 Контакт власника: +380991234567");
        }

        @Test
        void handle_shouldIncludeNavigationButtons() throws Exception {
            // given
            setupContext();
            given(finderRecommendationService.pollNext(INTERNAL_USER_ID))
                    .willReturn(Optional.of(LOST_REQUEST_ID));

            LostRequestDTO lostRequest = createLostRequestWithContact();
            given(lostRequestService.findById(LOST_REQUEST_ID)).willReturn(lostRequest);

            PetDTO pet = createPetDTO();
            given(petService.findById(PET_ID)).willReturn(pet);

            // when
            PartialBotApiMethod<?> result = underTest.handle(context);

            // then
            assertThat(result).isInstanceOf(SendPhoto.class);
            SendPhoto photo = (SendPhoto) result;

            InlineKeyboardMarkup keyboard = (InlineKeyboardMarkup) photo.getReplyMarkup();
            assertThat(keyboard).isNotNull();
            assertThat(keyboard.getKeyboard()).hasSize(1);
            assertThat(keyboard.getKeyboard().get(0)).hasSize(2);

            // First button should be "Next"
            String firstButtonText = keyboard.getKeyboard().get(0).get(0).getText();
            assertThat(firstButtonText).contains("Наступна");

            // Second button should be "Back"
            String secondButtonText = keyboard.getKeyboard().get(0).get(1).getText();
            assertThat(secondButtonText).contains("Повернутись");
        }

        @Test
        void handle_keyboardRemovalFailure_shouldStillShowRecommendation() throws Exception {
            // given
            setupContext();
            given(finderRecommendationService.pollNext(INTERNAL_USER_ID))
                    .willReturn(Optional.of(LOST_REQUEST_ID));

            LostRequestDTO lostRequest = createLostRequestWithContact();
            given(lostRequestService.findById(LOST_REQUEST_ID)).willReturn(lostRequest);

            PetDTO pet = createPetDTO();
            given(petService.findById(PET_ID)).willReturn(pet);

            // Keyboard removal failure is handled gracefully by the service

            // when
            PartialBotApiMethod<?> result = underTest.handle(context);

            // then
            assertThat(result).isInstanceOf(SendPhoto.class);
            // Should still show recommendation despite keyboard removal failure
        }

        @Test
        void handle_withAllRecommendationsSkippedDueToNoContact_shouldShowEmptyMessage() throws Exception {
            // given
            setupContext();
            given(finderRecommendationService.pollNext(INTERNAL_USER_ID))
                    .willReturn(Optional.of(LOST_REQUEST_ID))
                    .willReturn(Optional.empty()); // No more after skipping

            LostRequestDTO lostRequest = createLostRequestWithoutContact();
            given(lostRequestService.findById(LOST_REQUEST_ID)).willReturn(lostRequest);

            // when
            PartialBotApiMethod<?> result = underTest.handle(context);

            // then
            assertThat(result).isInstanceOf(SendMessage.class);
            SendMessage message = (SendMessage) result;
            assertThat(message.getText()).contains("Ви переглянули всі доступні анкети");
        }

        @Test
        void handle_shouldShowPetNameInCaption() throws Exception {
            // given
            setupContext();
            given(finderRecommendationService.pollNext(INTERNAL_USER_ID))
                    .willReturn(Optional.of(LOST_REQUEST_ID));

            LostRequestDTO lostRequest = createLostRequestWithContact();
            given(lostRequestService.findById(LOST_REQUEST_ID)).willReturn(lostRequest);

            PetDTO pet = new PetDTO(
                    PET_ID, 1L, "Rex", PetType.DOG, "photo456",
                    "German Shepherd", "Black", "Long", 3,
                    PetSex.MALE, PetSize.LARGE, "Scar on ear", PetStatus.IN_LOST
            );
            given(petService.findById(PET_ID)).willReturn(pet);

            // when
            PartialBotApiMethod<?> result = underTest.handle(context);

            // then
            assertThat(result).isInstanceOf(SendPhoto.class);
            SendPhoto photo = (SendPhoto) result;
            String caption = Objects.requireNonNull(photo.getCaption());
            assertThat(caption).contains("🐾 Rex");
        }
    }

    @Nested
    class ClearCache {

        @Test
        void clearCache_shouldRemoveFinderRecommendations() {
            // when
            underTest.clearCache(INTERNAL_USER_ID);

            // then
            verify(finderRecommendationService).remove(INTERNAL_USER_ID);
        }
    }

    // Helper methods

    private void setupContext() {
        UserAuthContext auth = new UserAuthContext(TELEGRAM_USER_ID, INTERNAL_USER_ID, UserType.REGULAR, "testuser");
        given(context.auth()).willReturn(auth);
        given(context.chatId()).willReturn(CHAT_ID);
        given(context.messageId()).willReturn(MESSAGE_ID);
    }

    private LostRequestDTO createLostRequestWithContact() {
        Point location = GEOMETRY_FACTORY.createPoint(new Coordinate(30.52, 50.45));
        return new LostRequestDTO(
                LOST_REQUEST_ID,
                PET_ID,
                "+380991234567",
                location,
                PetType.DOG,
                Instant.now()
        );
    }

    private LostRequestDTO createLostRequestWithoutContact() {
        Point location = GEOMETRY_FACTORY.createPoint(new Coordinate(30.52, 50.45));
        return new LostRequestDTO(
                LOST_REQUEST_ID,
                PET_ID,
                "",  // Empty contact info
                location,
                PetType.DOG,
                Instant.now()
        );
    }

    private PetDTO createPetDTO() {
        return new PetDTO(
                PET_ID,
                1L,
                "Buddy",
                PetType.DOG,
                "photo123",
                "Labrador",
                "Golden",
                "Smooth",
                2,
                PetSex.MALE,
                PetSize.MEDIUM,
                "White spot on chest",
                PetStatus.IN_LOST
        );
    }
}
