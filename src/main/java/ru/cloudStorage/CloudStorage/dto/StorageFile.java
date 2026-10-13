package ru.cloudStorage.CloudStorage.dto;

public record StorageFile(ResourceType type, String pathWithName, long size) implements StorageObject {
    @Override
    public boolean isDirectory() {
        return false;
    }
}
