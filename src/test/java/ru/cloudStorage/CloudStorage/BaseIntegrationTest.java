package ru.cloudStorage.CloudStorage;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MinIOContainer;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public abstract class BaseIntegrationTest {
    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected MinioClient minioClient;

    @Autowired
    private MinIOContainer minioContainer;

    @DynamicPropertySource
    static void minioProperties(DynamicPropertyRegistry registry, @Autowired MinIOContainer minio) {
        registry.add("minio.endpoint", minio::getS3URL);
        registry.add("minio.accessKey", minio::getUserName);
        registry.add("minio.secretKey", minio::getPassword);
    }
}
