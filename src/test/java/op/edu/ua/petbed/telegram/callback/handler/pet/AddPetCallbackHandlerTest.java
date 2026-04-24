package op.edu.ua.petbed.telegram.callback.handler.pet;

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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AddPetCallbackHandlerTest {

    @Mock
    FormService formService;

    @Mock
    CallbackQueryContext context;

    @InjectMocks
    AddPetCallbackHandler underTest;

    // .handle() -----------------------------------------------------------------

    @Nested
    class Handle {

        @Test
        void handle_startsFormAndReturnsMessage() {
            // given
            Long chatId = 123L;
            Long internalId = 1L;
            UserAuthContext auth = new UserAuthContext(123L, internalId, UserType.REGULAR, "testuser");

            given(context.auth()).willReturn(auth);
            given(context.chatId()).willReturn(chatId);
            given(context.callbackData()).willReturn(CallbackData.of(CallbackId.ADD_PET, null, 0));
            given(formService.startForm(FormType.ADD_PET, CallbackId.MY_PETS, internalId, chatId)).willReturn(null);

            // when
            BotApiMethod<?> result = underTest.handle(context);

            // then
            assertThat(result).isNull();
            verify(formService).startForm(FormType.ADD_PET, CallbackId.MY_PETS, internalId, chatId);
        }
    }
}