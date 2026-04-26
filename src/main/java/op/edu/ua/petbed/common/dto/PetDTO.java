package op.edu.ua.petbed.common.dto;

import op.edu.ua.petbed.common.model.PetSex;
import op.edu.ua.petbed.common.model.PetSize;
import op.edu.ua.petbed.common.model.PetStatus;
import op.edu.ua.petbed.common.model.PetType;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record PetDTO(
        Long id,
        Long ownerId,
        String name,
        PetType type,
        String photoId,
        String breed,
        String color,
        String colorPattern,
        Integer age,
        PetSex sex,
        PetSize size,
        String specialMarks,
        PetStatus status
) {

    public String formatInfo() {
        StringBuilder sb = new StringBuilder();

        sb.append("🐾 ").append(name).append("\n");

        if (!breed.isBlank()) {
            sb.append("🐩 Порода: ").append(breed).append("\n");
        }

        String fullColor = color;
        if (!colorPattern.isBlank()) {
            fullColor = color + ". " + colorPattern;
        }
        if (!fullColor.isBlank()) {
            sb.append("🎨 Колір: ").append(fullColor).append("\n");
        }

        String ageText;
        int lastDigit = age % 10;
        int lastTwoDigits = age % 100;
        if (lastDigit == 1 && lastTwoDigits != 11) {
            ageText = age + " рік";
        } else if (lastDigit >= 2 && lastDigit <= 4 && (lastTwoDigits < 12 || lastTwoDigits > 14)) {
            ageText = age + " роки";
        } else {
            ageText = age + " років";
        }
        sb.append("🎂 Вік: ").append(ageText).append("\n");

        String sizeText = switch (size) {
            case SMALL -> "малий";
            case MEDIUM -> "середній";
            case LARGE -> "великий";
        };
        sb.append("📏 Розмір: ").append(sizeText).append("\n");

        String statusText = switch (status) {
            case DEFAULT -> "звичайний";
            case IN_LOST -> "в пошуках";
            case IN_ADOPTION -> "шукає господаря";
            case IN_FOSTERING -> "шукає перетримку";
            case FOSTERED -> "на перетримці";
        };
        sb.append("🏷️ Статус: ").append(statusText).append("\n");

        if (!specialMarks.isBlank()) {
            sb.append("📝 Особливі прикмети: ").append(specialMarks).append("\n");
        }

        return sb.toString();
    }
}