package op.edu.ua.petbed.common.dto;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record LocationDTO(
        double latitude,
        double longitude
) {}