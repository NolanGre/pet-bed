package op.edu.ua.petbed.telegram.form.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.common.dto.CreatePetDTO;
import op.edu.ua.petbed.common.model.PetSex;
import op.edu.ua.petbed.common.model.PetSize;
import op.edu.ua.petbed.common.model.PetType;
import op.edu.ua.petbed.pet.PetService;
import op.edu.ua.petbed.telegram.form.FormData;
import op.edu.ua.petbed.telegram.form.scheme.FormStep;
import op.edu.ua.petbed.telegram.form.scheme.FormType;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import op.edu.ua.petbed.user.UserService;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;

import java.util.List;

@Slf4j
@Component
@NullMarked
@RequiredArgsConstructor
public class AddPetHandled implements FormSubmissionHandler {

    private final PetService petService;
    private final UserService userService;

    @Override
    public FormType getFormType() {
        return FormType.ADD_PET;
    }

    @Override
    public BotApiMethod<?> handle(FormData data) {
        log.debug("Add Pet Handled: {}", data);

        var userId = userService.findById(data.userId()).id();
        var result = petService.create(mapToDto(data, userId));

        return ResponseBuilder.sendMessage(data.chatId())
                .text("🟢 " + result.name() + " був доданий")
                .keyboard(InlineKeyboardBuilder.builder()
                        .backButtonTo(data.returnCallback())
                        .build())
                .build();
    }

    private static CreatePetDTO mapToDto(FormData data, Long userId) {
        List<FormStep> steps = FormType.ADD_PET.steps();
        return CreatePetDTO.builder()
                .ownerId(userId)
                .name(data.text(steps.get(0)))
                .type(PetType.valueOf(data.choice(steps.get(1)).toUpperCase()))
                .photoId(data.photo(steps.get(2)))
                .breed(data.text(steps.get(3)))
                .color(data.text(steps.get(4)))
                .colorPattern(data.text(steps.get(5)))
                .age(data.number(steps.get(6)))
                .sex(PetSex.valueOf(data.choice(steps.get(7)).toUpperCase()))
                .size(PetSize.valueOf(data.choice(steps.get(8)).toUpperCase()))
                .specialMarks(data.text(steps.get(9)))
                .build();
    }
}
