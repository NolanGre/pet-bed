package op.edu.ua.petbed.lost;

import op.edu.ua.petbed.lost.application.dto.FoundRequestDTO;
import op.edu.ua.petbed.common.model.PetType;
import org.jspecify.annotations.NullMarked;
import org.locationtech.jts.geom.Point;

import java.util.List;

@NullMarked
public interface FoundRequestService {

    FoundRequestDTO create(Long finderId, String photoUrl, PetType petType, Point location, String description);

    FoundRequestDTO findById(Long id);

    List<FoundRequestDTO> findByFinderId(Long finderId);
}
