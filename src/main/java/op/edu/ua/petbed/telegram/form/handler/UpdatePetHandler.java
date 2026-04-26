package op.edu.ua.petbed.telegram.form.handler;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.common.dto.UpdatePetDTO;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.common.model.PetSex;
import op.edu.ua.petbed.common.model.PetSize;
import op.edu.ua.petbed.pet.PetService;
import op.edu.ua.petbed.telegram.form.FormData;
import op.edu.ua.petbed.telegram.form.scheme.FormStep;
import op.edu.ua.petbed.telegram.form.scheme.FormType;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;

import java.util.List;
import java.util.function.Function;

@NullMarked
@Component
@RequiredArgsConstructor
public class UpdatePetHandler implements FormSubmissionHandler {

    private final PetService petService;

    @Override
    public FormType getFormType() {
        return FormType.UPDATE_PET;
    }

    @Override
    public BotApiMethod<?> handle(FormData data) {
        List<FormStep> s = FormType.UPDATE_PET.steps();
        Long petId = data.entityId();
        if (petId == null) {
            throw new PetBedException("Pet ID is null", PetBedException.ErrorCode.INTERNAL_ERROR);
        }

        petService.update(mapToDto(data, petId, s));

        return ResponseBuilder.sendMessage(data.chatId())
                .text("✅ Анкету оновлено")
                .keyboard(InlineKeyboardBuilder.builder()
                        .backButtonTo(data.returnCallback(), petId)
                        .build())
                .build();
    }

    private static UpdatePetDTO mapToDto(FormData data, Long petId, List<FormStep> s) {
        return UpdatePetDTO.builder()
                .id(petId)
                .name(data.textOrNull(s.getFirst()))
                .photoId(data.photoOrNull(s.get(1)))
                .breed(data.textOrNull(s.get(2)))
                .color(data.textOrNull(s.get(3)))
                .colorPattern(data.textOrNull(s.get(4)))
                .age(data.numberOrNull(s.get(5)))
                .sex(mapEnum(data.choiceOrNull(s.get(6)), PetSex::valueOf))
                .size(mapEnum(data.choiceOrNull(s.get(7)), PetSize::valueOf))
                .specialMarks(data.textOrNull(s.get(8)))
                .build();
    }


    @Nullable
    private static <E extends Enum<E>> E mapEnum(@Nullable String raw, Function<String, E> mapper) {
        return raw == null ? null : mapper.apply(raw.toUpperCase());
    }
}
