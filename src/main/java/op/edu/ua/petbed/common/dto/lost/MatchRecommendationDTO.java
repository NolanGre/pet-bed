package op.edu.ua.petbed.common.dto.lost;

import org.jspecify.annotations.NullMarked;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.Instant;

@NullMarked
public record MatchRecommendationDTO(
        Long matchQueueId,
        Long lostRequestId,
        Long foundRequestId,
        BigDecimal score,
        String photoUrl,
        String description,
        Point location,
        Instant foundRequestCreatedAt,
        Double distanceKm
) {}
