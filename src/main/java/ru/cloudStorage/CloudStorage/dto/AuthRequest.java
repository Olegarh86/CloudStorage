package ru.cloudStorage.CloudStorage.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Registration and authentication data")
public record AuthRequest(
        @NotBlank(message = "Username must not be empty")
        @Size(min = 5, max = 20, message = "Username must be between 5 and 20 characters")
        @Pattern(regexp = "^[a-zA-Z0-9]+[a-zA-Z_0-9]*[a-zA-Z0-9]+$", message = "Invalid username format")
        @JsonProperty(required = true)
        @Schema(description = "Unique username", example = "Junior")
        String username,
        @NotBlank(message = "The password must not be empty")
        @Size(min = 5, max = 20, message = "The password must be from 5 to 20 characters")
        @Pattern(regexp = "^[a-zA-Z0-9!@#$%^&*(),.?\\\":{}|<>[\\\\]/`~+=-_';]*$", message = "Password contains invalid characters")
        @JsonProperty(required = true)
        @Schema(description = "User password", example = "Junior123")
        String password) {
}
