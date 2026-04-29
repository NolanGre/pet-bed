package op.edu.ua.petbed.lost.application.matching;

import op.edu.ua.petbed.common.dto.PetDTO;
import op.edu.ua.petbed.common.model.PetSex;
import op.edu.ua.petbed.common.model.PetSize;
import op.edu.ua.petbed.common.model.PetStatus;
import op.edu.ua.petbed.common.model.PetType;
import op.edu.ua.petbed.lost.domain.model.FoundRequest;
import op.edu.ua.petbed.lost.domain.model.LostRequest;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MatchingAlgorithmTest {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private MatchingAlgorithm underTest;

    private static Point createPoint(double latitude, double longitude) {
        return GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
    }

    private static PetDTO createPetDTO(Long id, String name, PetType type) {
        return new PetDTO(
                id,
                1L,
                name,
                type,
                "photo123",
                "Labrador",
                "Black",
                "Solid",
                3,
                PetSex.MALE,
                PetSize.MEDIUM,
                "White spot on chest",
                PetStatus.DEFAULT
        );
    }

    private static LostRequest createLostRequest(Point location, String searchText) {
        PetDTO pet = createPetDTO(1L, "TestPet", PetType.DOG);
        LostRequest request = LostRequest.create(pet, "+380991234567", location);
        ReflectionTestUtils.setField(request, "searchText", searchText);
        return request;
    }

    private static FoundRequest createFoundRequest(Point location, String description) {
        return FoundRequest.create(1L, "photo.jpg", PetType.DOG, location, description);
    }

    @Test
    void calculateScore_withSameLocationAndText_returnsMaxScore() {
        // given
        Point location = createPoint(50.45, 30.52);
        String text = "golden retriever brown friendly";
        LostRequest lost = createLostRequest(location, text);
        FoundRequest found = createFoundRequest(location, text);

        given(jdbcTemplate.queryForObject(
                eq("SELECT ST_Distance(?::geography, ?::geography)"),
                eq(Double.class),
                any(),
                any()
        )).willReturn(0.0); // Same location = 0 meters

        given(jdbcTemplate.queryForObject(
                eq("SELECT similarity(?, ?)"),
                eq(Double.class),
                eq(text),
                eq(text)
        )).willReturn(1.0); // Same text = 100% similarity

        // when
        BigDecimal result = underTest.calculateScore(lost, found);

        // then
        // Formula: (geoScore * 0.4) + (textScore * 0.6) = (1.0 * 0.4) + (1.0 * 0.6) = 1.0
        assertThat(result).isEqualTo(new BigDecimal("1.0000"));
    }

    @Test
    void calculateScore_withMaxDistance_returnsLowGeoComponent() {
        // given
        Point lostLoc = createPoint(50.45, 30.52);
        Point foundLoc = createPoint(50.90, 31.00); // More than 50km away
        String text = "golden retriever";
        LostRequest lost = createLostRequest(lostLoc, text);
        FoundRequest found = createFoundRequest(foundLoc, text);

        given(jdbcTemplate.queryForObject(
                eq("SELECT ST_Distance(?::geography, ?::geography)"),
                eq(Double.class),
                any(),
                any()
        )).willReturn(50001.0); // Just over 50km

        given(jdbcTemplate.queryForObject(
                eq("SELECT similarity(?, ?)"),
                eq(Double.class),
                any(),
                any()
        )).willReturn(0.8);

        // when
        BigDecimal result = underTest.calculateScore(lost, found);

        // then
        // Formula: (0.0 * 0.4) + (0.8 * 0.6) = 0.48
        assertThat(result).isEqualTo(new BigDecimal("0.4800"));
    }

    @Test
    void calculateScore_combinesGeoAndTextCorrectly() {
        // given
        Point lostLoc = createPoint(50.45, 30.52);
        Point foundLoc = createPoint(50.46, 30.53); // ~1.57km away
        String searchText = "golden retriever brown";
        String description = "golden retriever light brown friendly";
        LostRequest lost = createLostRequest(lostLoc, searchText);
        FoundRequest found = createFoundRequest(foundLoc, description);

        double distanceMeters = 1570.0; // ~1.57km
        double geoScore = 1.0 - (distanceMeters / 50000.0); // = 0.9686
        double textSimilarity = 0.75;

        given(jdbcTemplate.queryForObject(
                eq("SELECT ST_Distance(?::geography, ?::geography)"),
                eq(Double.class),
                any(),
                any()
        )).willReturn(distanceMeters);

        given(jdbcTemplate.queryForObject(
                eq("SELECT similarity(?, ?)"),
                eq(Double.class),
                any(),
                any()
        )).willReturn(textSimilarity);

        // when
        BigDecimal result = underTest.calculateScore(lost, found);

        // then
        // Formula: (0.9686 * 0.4) + (0.75 * 0.6) = 0.38744 + 0.45 = 0.83744
        // Rounded to 4 decimal places: 0.8374
        double expectedScore = (geoScore * 0.4) + (textSimilarity * 0.6);
        assertThat(result.doubleValue()).isCloseTo(expectedScore, org.assertj.core.data.Offset.offset(0.0001));
    }

    @Test
    void calculateScore_withNullDistance_returnsZeroGeo() {
        // given
        Point lostLoc = createPoint(50.45, 30.52);
        Point foundLoc = createPoint(50.46, 30.53);
        String text = "golden retriever";
        LostRequest lost = createLostRequest(lostLoc, text);
        FoundRequest found = createFoundRequest(foundLoc, text);

        given(jdbcTemplate.queryForObject(
                eq("SELECT ST_Distance(?::geography, ?::geography)"),
                eq(Double.class),
                any(),
                any()
        )).willReturn(null); // Null from DB

        given(jdbcTemplate.queryForObject(
                eq("SELECT similarity(?, ?)"),
                eq(Double.class),
                any(),
                any()
        )).willReturn(0.6);

        // when
        BigDecimal result = underTest.calculateScore(lost, found);

        // then
        // Formula: (0.0 * 0.4) + (0.6 * 0.6) = 0.36
        assertThat(result).isEqualTo(new BigDecimal("0.3600"));
    }

    @Test
    void calculateScore_withNullSimilarity_returnsZeroText() {
        // given
        Point location = createPoint(50.45, 30.52);
        String text = "golden retriever";
        LostRequest lost = createLostRequest(location, text);
        FoundRequest found = createFoundRequest(location, text);

        given(jdbcTemplate.queryForObject(
                eq("SELECT ST_Distance(?::geography, ?::geography)"),
                eq(Double.class),
                any(),
                any()
        )).willReturn(0.0); // Same location

        given(jdbcTemplate.queryForObject(
                eq("SELECT similarity(?, ?)"),
                eq(Double.class),
                any(),
                any()
        )).willReturn(null); // Null from DB

        // when
        BigDecimal result = underTest.calculateScore(lost, found);

        // then
        // Formula: (1.0 * 0.4) + (0.0 * 0.6) = 0.4
        assertThat(result).isEqualTo(new BigDecimal("0.4000"));
    }

    @Test
    void calculateScore_returnsFourDecimalPlaces() {
        // given
        Point location = createPoint(50.45, 30.52);
        String text = "test";
        LostRequest lost = createLostRequest(location, text);
        FoundRequest found = createFoundRequest(location, text);

        given(jdbcTemplate.queryForObject(
                eq("SELECT ST_Distance(?::geography, ?::geography)"),
                eq(Double.class),
                any(),
                any()
        )).willReturn(12345.6789); // Arbitrary distance

        given(jdbcTemplate.queryForObject(
                eq("SELECT similarity(?, ?)"),
                eq(Double.class),
                any(),
                any()
        )).willReturn(0.123456789); // Many decimal places

        // when
        BigDecimal result = underTest.calculateScore(lost, found);

        // then
        assertThat(result.scale()).isEqualTo(4);

        // Verify string representation has exactly 4 decimal places
        String resultStr = result.toPlainString();
        int decimalIndex = resultStr.indexOf('.');
        assertThat(decimalIndex).isPositive();
        assertThat(resultStr.substring(decimalIndex + 1).length()).isEqualTo(4);
    }

    @Test
    void calculateScore_withDistanceExactly50km_returnsZeroGeoScore() {
        // given
        Point lostLoc = createPoint(50.45, 30.52);
        Point foundLoc = createPoint(50.90, 31.00);
        String text = "golden retriever";
        LostRequest lost = createLostRequest(lostLoc, text);
        FoundRequest found = createFoundRequest(foundLoc, text);

        given(jdbcTemplate.queryForObject(
                eq("SELECT ST_Distance(?::geography, ?::geography)"),
                eq(Double.class),
                any(),
                any()
        )).willReturn(50000.0); // Exactly 50km

        given(jdbcTemplate.queryForObject(
                eq("SELECT similarity(?, ?)"),
                eq(Double.class),
                any(),
                any()
        )).willReturn(0.5);

        // when
        BigDecimal result = underTest.calculateScore(lost, found);

        // then
        // Formula: (0.0 * 0.4) + (0.5 * 0.6) = 0.3
        assertThat(result).isEqualTo(new BigDecimal("0.3000"));
    }

    @Test
    void calculateScore_withDistanceAt25km_returnsHalfGeoScore() {
        // given
        Point lostLoc = createPoint(50.45, 30.52);
        Point foundLoc = createPoint(50.675, 30.77);
        String text = "golden retriever";
        LostRequest lost = createLostRequest(lostLoc, text);
        FoundRequest found = createFoundRequest(foundLoc, text);

        given(jdbcTemplate.queryForObject(
                eq("SELECT ST_Distance(?::geography, ?::geography)"),
                eq(Double.class),
                any(),
                any()
        )).willReturn(25000.0); // Exactly 25km

        given(jdbcTemplate.queryForObject(
                eq("SELECT similarity(?, ?)"),
                eq(Double.class),
                any(),
                any()
        )).willReturn(0.5);

        // when
        BigDecimal result = underTest.calculateScore(lost, found);

        // then
        // Geo score: 1.0 - (25000 / 50000) = 0.5
        // Formula: (0.5 * 0.4) + (0.5 * 0.6) = 0.2 + 0.3 = 0.5
        assertThat(result).isEqualTo(new BigDecimal("0.5000"));
    }
}
