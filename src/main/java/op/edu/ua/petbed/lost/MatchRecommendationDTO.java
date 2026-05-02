package op.edu.ua.petbed.lost;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Public DTO for match recommendation data.
 * Exposed as part of the lost module's public API.
 */
@NullMarked
public record MatchRecommendationDTO(
        Long matchQueueId,
        Long lostRequestId,
        Long foundRequestId,
        BigDecimal score,
        String photoUrl,
        @Nullable String breedText,
        @Nullable String colorText,
        @Nullable String coatText,
        @Nullable String sizeText,
        @Nullable String sexText,
        @Nullable String featuresText,
        Point location,
        Instant foundRequestCreatedAt,
        Double distanceKm
) {}