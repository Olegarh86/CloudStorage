package ru.cloudStorage.CloudStorage.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ru.cloudStorage.CloudStorage.api.UserApi;
import ru.cloudStorage.CloudStorage.dto.AuthResponse;
import ru.cloudStorage.CloudStorage.service.AuthService;

@RestController
public class UserController implements UserApi {
    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public ResponseEntity<AuthResponse> getUsername() {
        String username = authService.getUsername();
        return ResponseEntity.status(HttpStatus.OK).body(new AuthResponse(username));
    }
}
