package ru.cloudStorage.CloudStorage.dto;

public interface StorageObject {
    ResourceType type();
    String pathWithName();
    boolean isDirectory();
}
