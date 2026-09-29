package ru.cloudStorage.CloudStorage.dto;

import java.io.InputStream;

public record UploadDto(InputStream inputStream, long objectSize, int partSize, String contentType) {
}
