package ru.cloudStorage.CloudStorage.dto;

public record StorageDirectory(ResourceType type, String pathWithName) implements StorageObject {
    @Override
    public boolean isDirectory() {
        return true;
    }
}
