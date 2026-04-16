package op.edu.ua.petbed.telegram.callback;

import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

public record CallbackData(
        int callbackId,
        @Nullable Long entityId,
        @Nullable Integer offset
) {
    public static CallbackData from(@Nullable String data) {
        if (StringUtils.isBlank(data)) {
            return new CallbackData(0, null, null);
        }
        String[] parts = data.split(",", -1);
        try {
            int id = Integer.parseInt(parts[0].trim());
            Long entity = parts.length > 1 && !parts[1].isBlank() ? Long.parseLong(parts[1].trim()) : null;
            Integer off = parts.length > 2 && !parts[2].isBlank() ? Integer.parseInt(parts[2].trim()) : null;
            return new CallbackData(id, entity, off);
        } catch (NumberFormatException e) {
            return new CallbackData(0, null, null);
        }
    }

    public static CallbackData of(CallbackId callbackId, @Nullable Long entityId, @Nullable Integer offset) {
        return new CallbackData(callbackId.id(), entityId, offset);
    }

    public Optional<CallbackId> callbackIdEnum() {
        return CallbackId.fromId(callbackId);
    }

    @Override
    public @NonNull String toString() {
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
