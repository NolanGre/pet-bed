package op.edu.ua.petbed.telegram.response;

import op.edu.ua.petbed.telegram.callback.CallbackId;

public record CallbackListItem(CallbackId callbackId, Long entityId, String label) {
}
