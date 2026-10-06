package ru.cloudStorage.CloudStorage.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.cloudStorage.CloudStorage.api.AuthenticationApi;
import ru.cloudStorage.CloudStorage.dto.AuthRequest;
import ru.cloudStorage.CloudStorage.dto.AuthResponse;
import ru.cloudStorage.CloudStorage.model.User;
import ru.cloudStorage.CloudStorage.service.AuthService;
import ru.cloudStorage.CloudStorage.service.MinIOService;

@RestController
public class AuthController implements AuthenticationApi {

    private final HttpServletRequest request;
    private final AuthService authService;
    private final MinIOService minioService;

    @Autowired
    public AuthController(HttpServletRequest request, AuthService authService, MinIOService minioService) {
        this.request = request;
        this.authService = authService;
        this.minioService = minioService;
    }

    @Override
    public ResponseEntity<AuthResponse> authenticate(AuthRequest authRequest) {
        authService.login(authRequest, request);
        return ResponseEntity.status(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(new AuthResponse(authRequest.username()));
    }

    @Override
    public ResponseEntity<AuthResponse> createNewUser(AuthRequest authRequest) {
        User newUser = authService.createNewUser(authRequest);
        authService.login(authRequest, request);
        minioService.createNewRootFolder(newUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(newUser.getUserName()));
    }
}
