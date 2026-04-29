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

    private static final double GEO_WEIGHT = 0.4;
    private static final double TEXT_WEIGHT = 0.6;
    private static final double MAX_DISTANCE_KM = 50.0;

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
        double textScore = calculateTextScore(lost.getSearchText(), found.getDescription());

        double totalScore = (geoScore * GEO_WEIGHT) + (textScore * TEXT_WEIGHT);

        return BigDecimal.valueOf(totalScore).setScale(4, RoundingMode.HALF_UP);
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

    /**
     * Calculates the text similarity score using PostgreSQL pg_trgm.
     * Uses the similarity() function which returns 0.0 to 1.0.
     *
     * @param searchText  the search text from the lost request
     * @param description the description from the found request
     * @return similarity score between 0.0 and 1.0
     */
    private double calculateTextScore(String searchText, String description) {
        String sql = "SELECT similarity(?, ?)";
        Double similarity = jdbcTemplate.queryForObject(
                sql,
                Double.class,
                searchText,
                description
        );

        return similarity != null ? similarity : 0.0;
    }
}
