package op.edu.ua.petbed;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PetBedApplication {

    public static void main(String[] args) {
        SpringApplication.run(PetBedApplication.class, args);
    }

}
