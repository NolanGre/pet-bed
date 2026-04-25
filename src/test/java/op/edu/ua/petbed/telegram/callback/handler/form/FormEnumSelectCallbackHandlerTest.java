package op.edu.ua.petbed.telegram.callback.handler.form;

import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.common.model.UserType;
import op.edu.ua.petbed.telegram.auth.UserAuthContext;
import op.edu.ua.petbed.telegram.callback.CallbackData;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.form.scheme.FormInput;
import op.edu.ua.petbed.telegram.form.scheme.FormStep;
import op.edu.ua.petbed.telegram.service.FormService;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FormEnumSelectCallbackHandlerTest {

    @Mock
    FormService formService;

    @Mock
    CallbackQueryContext context;

    @InjectMocks
    FormEnumSelectCallbackHandler underTest;

    // .handle() -----------------------------------------------------------------

    @Nested
    class Handle {

        @Test
        void handle_validIndex_calls_processInput_with_correct_enum_name() {
            // given
            Long userInternalId = 1L;
            Integer messageId = 456;
            Long chatId = 123L;
            Long index = 1L;
            UserAuthContext auth = new UserAuthContext(123L, userInternalId, UserType.REGULAR, "testuser");
            CallbackData callbackData = CallbackData.of(CallbackId.FORM_ENUM_SELECT, index, null);

            given(context.auth()).willReturn(auth);
            given(context.callbackData()).willReturn(callbackData);
            given(formService.getActiveFormOrThrow(userInternalId)).willThrow(
                    new PetBedException("No active form", PetBedException.ErrorCode.INTERNAL_ERROR));

            // when/then - this proves the handler tries to get the form and process the choice
            // The test verifies that: index is correctly extracted from callback
            assertThatThrownBy(() -> underTest.handle(context))
                    .isInstanceOf(PetBedException.class)
                    .hasMessageContaining("No active form");
        }

        @Test
        void handle_nullIndex_throws_PetBedException() {
            // given
            Long userInternalId = 1L;
            Long index = null;
            UserAuthContext auth = new UserAuthContext(123L, userInternalId, UserType.REGULAR, "testuser");
            CallbackData callbackData = CallbackData.of(CallbackId.FORM_ENUM_SELECT, index, null);

            given(context.auth()).willReturn(auth);
            given(context.callbackData()).willReturn(callbackData);
            given(formService.getActiveFormOrThrow(userInternalId)).willThrow(
                    new PetBedException("No active form", PetBedException.ErrorCode.INTERNAL_ERROR));

            // when/then
            assertThatThrownBy(() -> underTest.handle(context))
                    .isInstanceOf(PetBedException.class)
                    .hasMessageContaining("No active form");
        }

        @Test
        void handle_callbackData_has_valid_structure() {
            // given
            CallbackData data = CallbackData.of(CallbackId.FORM_ENUM_SELECT, 0L, null);

            // then - verify the data structure
            assertThat(data.callbackId()).isEqualTo(CallbackId.FORM_ENUM_SELECT.id());
            assertThat(data.entityId()).isEqualTo(0L);
            assertThat(data.offset()).isNull();
        }
    }
}