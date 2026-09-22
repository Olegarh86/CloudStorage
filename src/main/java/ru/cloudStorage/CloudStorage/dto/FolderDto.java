package ru.cloudStorage.CloudStorage.dto;

public record FolderDto(String fullPath, String bucketName, String rootPath, String pathWithoutName, String name) {
}
