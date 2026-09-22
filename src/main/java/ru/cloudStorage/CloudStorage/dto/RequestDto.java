package ru.cloudStorage.CloudStorage.dto;

public record RequestDto(String bucketName, String rootPath, String decodedPath) {
}
