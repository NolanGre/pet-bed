package op.edu.ua.petbed.lost;

import op.edu.ua.petbed.common.dto.LostRequestDTO;
import org.jspecify.annotations.NullMarked;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@NullMarked
public interface LostRequestService {

    LostRequestDTO create(Long petId, String contactInfo, Point location);

    LostRequestDTO findById(Long id);

    List<LostRequestDTO> findActiveByOwnerId(Long ownerId);

    Page<LostRequestDTO> findByOwnerId(Long ownerId, Pageable pageable);

    void cancel(Long lostRequestId);

    boolean existsByPetId(Long petId);
}
