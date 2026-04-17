package op.edu.ua.petbed.telegram.callback;

import op.edu.ua.petbed.common.exceptions.PetBedException;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record CallbackData(
        int callbackId,
        @Nullable Long entityId,
        @Nullable Integer offset
) {
    public static CallbackData from(@Nullable String data) {
        if (StringUtils.isBlank(data)) {
            throw new PetBedException("Callback data is required", PetBedException.ErrorCode.INVALID_CALLBACK);
        }
        String[] parts = data.split(",", -1);
        int id = Integer.parseInt(parts[0].trim());
        Long entity = parts.length > 1 && !parts[1].isBlank() ? Long.parseLong(parts[1].trim()) : null;
        Integer off = parts.length > 2 && !parts[2].isBlank() ? Integer.parseInt(parts[2].trim()) : null;
        return new CallbackData(id, entity, off);
    }

    public static CallbackData of(CallbackId callbackId, @Nullable Long entityId, @Nullable Integer offset) {
        return new CallbackData(callbackId.id(), entityId, offset);
    }

    public static CallbackData of(CallbackId callbackId) {
        return new CallbackData(callbackId.id(), null, null);
    }

    public CallbackData withPrevPage() {
        int newOffset = (offset != null ? offset : 0) - 1;
        return new CallbackData(callbackId, entityId, newOffset);
    }

    public CallbackData withNextPage() {
        int newOffset = (offset != null ? offset : 0) + 1;
        return new CallbackData(callbackId, entityId, newOffset);
    }

    public CallbackId callbackIdEnum() {
        return CallbackId.fromId(callbackId);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(callbackId);
        sb.append(",");
        if (entityId != null) {
            sb.append(entityId);
        }
        sb.append(",");
        if (offset != null) {
            sb.append(offset);
        }
        return sb.toString();
    }
}
