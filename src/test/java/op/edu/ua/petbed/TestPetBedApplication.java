package op.edu.ua.petbed;

import org.springframework.boot.SpringApplication;

public class TestPetBedApplication {

    static void main(String[] args) {
        SpringApplication.from(PetBedApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
