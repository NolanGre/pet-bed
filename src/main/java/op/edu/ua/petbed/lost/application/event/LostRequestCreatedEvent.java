package op.edu.ua.petbed.lost.application.event;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record LostRequestCreatedEvent(Long lostRequestId) {
}
