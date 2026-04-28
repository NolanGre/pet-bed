package op.edu.ua.petbed.lost.domain.model;

public enum ViewingStatus {
    NEW,
    VIEWED,
    REJECTED;

    public boolean isNew() {
        return this == NEW;
    }

    public boolean isViewed() {
        return this == VIEWED;
    }

    public boolean isRejected() {
        return this == REJECTED;
    }
}
