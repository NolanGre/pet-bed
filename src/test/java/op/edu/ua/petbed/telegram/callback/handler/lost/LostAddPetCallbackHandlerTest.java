package op.edu.ua.petbed.telegram.callback.handler.lost;

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

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LostAddPetCallbackHandlerTest {

    @Mock
    FormService formService;

    @Mock
    CallbackQueryContext context;

    @InjectMocks
    LostAddPetCallbackHandler underTest;

    @Nested
    class Handle {

        @Test
        void handle_startsAddPetFormWithReturnToLostStart() {
            // given
            Long userId = 1L;
            Long chatId = 123L;
            UserAuthContext auth = new UserAuthContext(123L, userId, UserType.REGULAR, "user");
            CallbackData callbackData = new CallbackData(CallbackId.LOST_ADD_PET.id(), null, null);

            given(context.auth()).willReturn(auth);
            given(context.chatId()).willReturn(chatId);

            // when
            underTest.handle(context);

            // then
            verify(formService).startCreateForm(
                    FormType.ADD_PET,
                    CallbackId.LOST_START,
                    userId,
                    chatId
            );
        }
    }}
