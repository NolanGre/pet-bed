package op.edu.ua.petbed.lost.application.event;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record FoundRequestCreatedEvent(Long foundRequestId) {
}
