package ru.cloudStorage.CloudStorage.dto;

public record RenameRequestDto(String bucketName, String rootPath, String decodedPathFrom, String decodedPathTo) {
}
