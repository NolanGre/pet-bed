package op.edu.ua.petbed.telegram.response;

import op.edu.ua.petbed.telegram.callback.CallbackId;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record CallbackListItem(CallbackId callbackId, Long entityId, String label) {
}
