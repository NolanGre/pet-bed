package op.edu.ua.petbed.fostering.domain.model;

import op.edu.ua.petbed.common.model.AdoptionPostStatus;

public enum FosteringPostStatus {
    ACTIVE,
    PENDING_CONFIRMATION,
    COMPLETED,
    CANCELLED;

    public boolean showsResponses() {
        return this == ACTIVE || this == PENDING_CONFIRMATION;
    }
}
