package ru.cloudStorage.CloudStorage;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MinIOContainer;
import ru.cloudStorage.CloudStorage.dto.AuthRequest;
import ru.cloudStorage.CloudStorage.repository.UserRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public abstract class BaseIntegrationTest {
    protected final ObjectMapper mapper = new ObjectMapper();
    protected final String name = "TestUser";
    protected final String password = "TestUser123";
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected MinioClient minioClient;

    @Autowired
    protected MinIOContainer minioContainer;
}
