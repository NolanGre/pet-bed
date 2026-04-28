package op.edu.ua.petbed.lost.domain.model;

public enum ViewingStatus {
    NEW,
    VIEWED,
    CONFIRMED;

    public boolean isNew() {
        return this == NEW;
    }

    public boolean isViewed() {
        return this == VIEWED;
    }
}
