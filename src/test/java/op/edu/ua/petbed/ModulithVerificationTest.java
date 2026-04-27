package op.edu.ua.petbed;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ModulithVerificationTest {

    @Test
    void modularStructure_is_valid() {
        var modules = ApplicationModules.of(PetBedApplication.class);

        modules.verify();
    }
}