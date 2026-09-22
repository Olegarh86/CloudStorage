package ru.cloudStorage.CloudStorage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "Object information")
public class ResourceResponseDto {
    @Schema(description = "Path to object", example = "folder1/folder2/", requiredMode = Schema.RequiredMode.REQUIRED)
    private final String path;
    @Schema(description = "Object name", example = "file.txt", requiredMode = Schema.RequiredMode.REQUIRED)
    private final String name;
    @Schema(description = "Object size (only for ResourceType = FILE)", example = "123")
    private final Long size;
    @Schema(description = "Object type (DIRECTORY or FILE)", example = "FILE", requiredMode = Schema.RequiredMode.REQUIRED)
    private final ResourceType type;

    public ResourceResponseDto(String path, String name, Long size) {
        this.path = path;
        this.name = name;
        this.size = size;
        this.type = ResourceType.FILE;
    }

    public ResourceResponseDto(String path, String name) {
        this.path = path;
        this.name = name;
        this.size = null;
        this.type = ResourceType.DIRECTORY;
    }
}
