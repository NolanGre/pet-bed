package op.edu.ua.petbed.common.model;

public enum PetStatus {
    DEFAULT,
    IN_LOST,
    IN_ADOPTION,
    IN_FOSTERING,
    FOSTERED;

    public boolean canUpdate() {
        return this == DEFAULT;
    }

    public boolean canDelete() {
        return this != FOSTERED;
    }
}