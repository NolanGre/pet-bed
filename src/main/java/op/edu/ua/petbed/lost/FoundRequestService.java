package op.edu.ua.petbed.lost;

import op.edu.ua.petbed.common.dto.FoundRequestDTO;
import op.edu.ua.petbed.lost.application.dto.CreateFoundRequestDTO;
import org.jspecify.annotations.NullMarked;

import java.util.List;

@NullMarked
public interface FoundRequestService {

    FoundRequestDTO create(CreateFoundRequestDTO dto);

    FoundRequestDTO findById(Long id);

    List<FoundRequestDTO> findByFinderId(Long finderId);
}
