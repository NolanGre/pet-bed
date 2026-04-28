package op.edu.ua.petbed.lost.domain.repository;

import op.edu.ua.petbed.lost.domain.model.FoundRequest;
import org.jspecify.annotations.NullMarked;
import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@NullMarked
public interface FoundRequestRepository extends JpaRepository<FoundRequest, Long> {

    List<FoundRequest> findByFinderId(Long finderId);

    @Query(value = """
            SELECT fr.* FROM found_requests fr
            WHERE fr.pet_type = :petType
            AND ST_DWithin(fr.location, :location, 50000)
            AND fr.created_at > :since
            ORDER BY fr.created_at DESC
            """, nativeQuery = true)
    List<FoundRequest> findRecentByPetTypeAndLocation(
            @Param("petType") String petType,
            @Param("location") Point location,
            @Param("since") Timestamp since
    );

    @Modifying
    @Query("DELETE FROM FoundRequest fr WHERE fr.createdAt < :date")
    void deleteOlderThan(@Param("date") Instant date);
}
