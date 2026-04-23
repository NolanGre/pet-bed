package op.edu.ua.petbed.telegram.callback.handler.pet;

import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.response.CallbackListItem;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;

import java.util.List;

@NullMarked
@Component
public class MyPetsCallbackHandler implements CallbackHandler {

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.MY_PETS;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        //call pet service, get list of pets
        Page<CallbackListItem> page = new PageImpl<>(List.of(
                new CallbackListItem(CallbackId.PET_DETAIL, 1L, "Барсик"),
                new CallbackListItem(CallbackId.PET_DETAIL, 2L, "Бебрик")
        ), PageRequest.of(0, 5), 10);

        return ResponseBuilder.telegram()
                .chatId(context.chatId())
                .text("🐾 Мої улюбленці")
                .keyboard(InlineKeyboardBuilder.builder()
                        .paginatedList(page, context.callbackData())
                        .navButtonsFor(getCallbackId())
                        .backButtonFor(getCallbackId())
                        .build())
                .editMessage(context.messageId())
                .build();
    }
}
