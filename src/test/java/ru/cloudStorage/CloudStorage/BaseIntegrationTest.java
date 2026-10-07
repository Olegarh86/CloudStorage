package ru.cloudStorage.CloudStorage;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.messages.Item;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.cloudStorage.CloudStorage.repository.UserRepository;


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

    @AfterEach
    public void clearMinio() {
        Iterable<Result<Item>> result = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket("test-user-files")
                        .prefix("")
                        .recursive(true)
                        .build());

        for (Result<Item> item : result) {
            try {
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket("test-user-files")
                                .object(item.get().objectName())
                                .build()
                );
            } catch (Exception e) {
                throw new RuntimeException("Error cleaning up test bucket");
            }
        }
    }
}
