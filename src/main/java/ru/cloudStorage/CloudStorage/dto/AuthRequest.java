package ru.cloudStorage.CloudStorage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Registration and authentication data")
public record AuthRequest(
        @NotBlank(message = "Имя пользователя не должно быть пустым")
        @Size(min = 5, max = 20, message = "Имя пользователя должно быть от 5 до 20 символов")
        @Pattern(regexp = "^[a-zA-Z0-9]+[a-zA-Z_0-9]*[a-zA-Z0-9]+$", message = "Неверный формат имени пользователя")
        @Schema(description = "Unique username", example = "Junior", requiredMode = Schema.RequiredMode.REQUIRED)
        String username,
        @NotBlank(message = "Пароль не должен быть пустым")
        @Size(min = 5, max = 20, message = "Пароль должен быть от 5 до 20 символов")
        @Pattern(regexp = "^[a-zA-Z0-9!@#$%^&*(),.?\\\":{}|<>[\\\\]/`~+=-_';]*$", message = "Пароль содержит недопустимые символы")
        @Schema(description = "User password", example = "Junior123", requiredMode = Schema.RequiredMode.REQUIRED)
        String password) {
}
