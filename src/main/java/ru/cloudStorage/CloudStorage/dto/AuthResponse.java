package ru.cloudStorage.CloudStorage.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Returns the name of the authenticated user")
public record AuthResponse(
        @Schema(description = "Authenticated user name", example = "Junior", requiredMode = Schema.RequiredMode.REQUIRED)
        String username) {
}
