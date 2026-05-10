package op.edu.ua.petbed.common.dto;

import org.jspecify.annotations.NullMarked;

import java.util.List;

@NullMarked
public record FosteringPostDetailDTO(
        FosteringPostDTO post,
        List<FosteringResponseDTO> responses
) {}
