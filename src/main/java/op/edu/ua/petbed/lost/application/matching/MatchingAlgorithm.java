package op.edu.ua.petbed.lost.application.matching;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.lost.domain.model.FoundRequest;
import op.edu.ua.petbed.lost.domain.model.LostRequest;
import org.jspecify.annotations.NullMarked;
import org.locationtech.jts.geom.Point;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Algorithm for calculating matching scores between lost and found requests.
 * Combines geographic proximity (40%) and text similarity (60%) into a final score.
 *
 * <p>Geographic component uses linear scale: 0km = 100%, 50km = 0%
 * <p>Text component uses PostgreSQL pg_trgm similarity function
 */
@NullMarked
@Component
@RequiredArgsConstructor
public class MatchingAlgorithm {

    private static final double GEO_WEIGHT = 0.6;
    private static final double TEXT_WEIGHT = 0.4;
    private static final double MAX_DISTANCE_KM = 50.0;

    // Text matching field weights
    private static final double BREED_WEIGHT = 0.35;
    private static final double COLOR_WEIGHT = 0.30;
    private static final double COAT_WEIGHT = 0.15;
    private static final double SIZE_WEIGHT = 0.10;
    private static final double SEX_WEIGHT = 0.10;

    private final JdbcTemplate jdbcTemplate;

    /**
     * Calculates the overall matching score between a lost request and a found request.
     *
     * <p>Formula: (geoScore * 0.4) + (textScore * 0.6)
     *
     * @param lost  the lost request
     * @param found the found request
     * @return score between 0.0000 and 1.0000 (4 decimal places)
     */
    public BigDecimal calculateScore(LostRequest lost, FoundRequest found) {
        double geoScore = calculateGeoScore(lost.getLastSeenLocation(), found.getLocation());
        double textScore = calculateGroupedTextScore(lost, found);

        double totalScore = (geoScore * GEO_WEIGHT) + (textScore * TEXT_WEIGHT);

        return BigDecimal.valueOf(totalScore).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Calculates grouped text similarity score using individual attribute fields.
     * Uses field-specific weights defined as constants (BREED_WEIGHT, COLOR_WEIGHT, etc.).
     */
    private double calculateGroupedTextScore(LostRequest lost, FoundRequest found) {
        double totalWeight = 0;
        double weightedScore = 0;

        if (lost.getBreedText() != null && found.getBreedText() != null) {
            double score = calculateSimilarity(lost.getBreedText(), found.getBreedText());
            weightedScore += score * BREED_WEIGHT;
            totalWeight += BREED_WEIGHT;
        }

        if (lost.getColorText() != null && found.getColorText() != null) {
            double score = calculateSimilarity(lost.getColorText(), found.getColorText());
            weightedScore += score * COLOR_WEIGHT;
            totalWeight += COLOR_WEIGHT;
        }

        if (lost.getCoatText() != null && found.getCoatText() != null) {
            double score = calculateSimilarity(lost.getCoatText(), found.getCoatText());
            weightedScore += score * COAT_WEIGHT;
            totalWeight += COAT_WEIGHT;
        }

        if (lost.getSizeText() != null && found.getSizeText() != null) {
            double score = lost.getSizeText().equalsIgnoreCase(found.getSizeText()) ? 1.0 : 0.0;
            weightedScore += score * SIZE_WEIGHT;
            totalWeight += SIZE_WEIGHT;
        }

        if (lost.getSexText() != null && found.getSexText() != null) {
            double score = lost.getSexText().equalsIgnoreCase(found.getSexText()) ? 1.0 : 0.0;
            weightedScore += score * SEX_WEIGHT;
            totalWeight += SEX_WEIGHT;
        }

        if (totalWeight == 0) {
            return 0.0;
        }

        return weightedScore / totalWeight;
    }

    private double calculateSimilarity(String text1, String text2) {
        String sql = "SELECT similarity(?, ?)";
        Double similarity = jdbcTemplate.queryForObject(sql, Double.class, text1, text2);
        return similarity != null ? similarity : 0.0;
    }

    /**
     * Calculates the geographic score based on distance between two points.
     * Uses PostGIS ST_Distance function.
     *
     * <p>Linear scale: 0km = 100% (score 1.0), 50km = 0% (score 0.0)
     *
     * @param lostLoc  the location from the lost request
     * @param foundLoc the location from the found request
     * @return score between 0.0 and 1.0
     */
    private double calculateGeoScore(Point lostLoc, Point foundLoc) {
        String sql = "SELECT ST_Distance(?::geography, ?::geography)";
        Double distanceMeters = jdbcTemplate.queryForObject(
                sql,
                Double.class,
                toWkt(lostLoc),
                toWkt(foundLoc)
        );

        if (distanceMeters == null || distanceMeters > MAX_DISTANCE_KM * 1000) {
            return 0.0;
        }

        // Linear scale: 50km = 0%, 0km = 100%
        return 1.0 - (distanceMeters / (MAX_DISTANCE_KM * 1000));
    }

    /**
     * Converts a JTS Point to WKT (Well-Known Text) format for PostGIS.
     * Format: SRID=4326;POINT(x y)
     *
     * @param point the point to convert
     * @return WKT string representation
     */
    private String toWkt(Point point) {
        return String.format("SRID=4326;POINT(%f %f)", point.getX(), point.getY());
    }

}
