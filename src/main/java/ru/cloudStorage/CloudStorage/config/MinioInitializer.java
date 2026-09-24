package ru.cloudStorage.CloudStorage.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ru.cloudStorage.CloudStorage.exception.MinioInitializeException;


@Component
public class MinioInitializer implements CommandLineRunner {
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    public MinioInitializer(MinioClient minioClient, MinioProperties minioProperties) {
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
    }

    @Override
    public void run(@NotNull String... args) {
        try {
            boolean found =
                    minioClient.bucketExists(BucketExistsArgs.builder().bucket(minioProperties.getBucketName()).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioProperties.getBucketName()).build());
                System.out.printf("Bucket with name: %s created.", minioProperties.getBucketName());
            } else {
                System.out.printf("Bucket %s already exists.", minioProperties.getBucketName());
            }
        } catch (Exception e) {
            throw new MinioInitializeException(e);
        }
    }
}
