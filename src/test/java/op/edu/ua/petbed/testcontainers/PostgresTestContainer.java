package op.edu.ua.petbed.testcontainers;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
public class PostgresTestContainer {

    @Container
    @ServiceConnection
    public static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName
            .parse("postgis/postgis:17-3.5-alpine")
            .asCompatibleSubstituteFor("postgres"));
}
