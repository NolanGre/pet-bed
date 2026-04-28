package op.edu.ua.petbed.lost.domain.model;

public enum LostRequestStatus {
    ACTIVE,
    CANCELLED;

    public boolean isActive() {
        return this == ACTIVE;
    }
}
