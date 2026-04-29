package op.edu.ua.petbed.telegram.callback.handler.lost;

import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.common.model.UserType;
import op.edu.ua.petbed.telegram.auth.UserAuthContext;
import op.edu.ua.petbed.telegram.callback.CallbackData;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.form.scheme.FormType;
import op.edu.ua.petbed.telegram.service.FormService;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LostSelectPetCallbackHandlerTest {

    @Mock
    FormService formService;

    @Mock
    CallbackQueryContext context;

    @InjectMocks
    LostSelectPetCallbackHandler underTest;

    @Nested
    class Handle {

        @Test
        void handle_withValidPetId_startsCreateLostRequestForm() {
            // given
            Long userId = 1L;
            Long chatId = 123L;
            Long petId = 42L;
            UserAuthContext auth = new UserAuthContext(123L, userId, UserType.REGULAR, "user");
            CallbackData callbackData = new CallbackData(CallbackId.LOST_SELECT_PET.id(), petId, null);

            given(context.auth()).willReturn(auth);
            lenient().when(context.chatId()).thenReturn(chatId);
            given(context.callbackData()).willReturn(callbackData);

            // when
            underTest.handle(context);

            // then
            verify(formService).startUpdateForm(
                    FormType.CREATE_LOST_REQUEST,
                    CallbackId.LOST_START,
                    userId,
                    chatId,
                    petId
            );
        }

        @Test
        void handle_withoutPetId_throwsException() {
            // given
            CallbackData callbackData = new CallbackData(CallbackId.LOST_SELECT_PET.id(), null, null);

            given(context.callbackData()).willReturn(callbackData);

            // when/then
            assertThatThrownBy(() -> underTest.handle(context))
                    .isInstanceOf(PetBedException.class)
                    .hasMessageContaining("Pet ID is required");
        }
    }
}
