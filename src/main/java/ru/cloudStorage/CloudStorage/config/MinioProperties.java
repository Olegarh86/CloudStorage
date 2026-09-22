package ru.cloudStorage.CloudStorage.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@NoArgsConstructor
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {
    private String accessKey;
    private String secretKey;
    private String endpoint;
    private String bucketName;
}
