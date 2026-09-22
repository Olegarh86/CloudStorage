package ru.cloudStorage.CloudStorage.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.cloudStorage.CloudStorage.dto.AuthRequest;
import ru.cloudStorage.CloudStorage.dto.AuthResponse;
import ru.cloudStorage.CloudStorage.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Methods for new user registration and authentication")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/sign-up")
    @Operation(summary = "Registration new user",
            description = "Accepts username and password, hashes the password and stores the user in the DB. Create " +
                          "session and cookie")
    @ApiResponse(responseCode = "201", description = "User successfully created")
    @ApiResponse(responseCode = "400", description = "Validation errors")
    @ApiResponse(responseCode = "409", description = "Username is busy")
    @ApiResponse(responseCode = "500", description = "Unknown error")
    public ResponseEntity<AuthResponse> createNewUser(@RequestBody @Valid AuthRequest authRequest, HttpServletRequest request) {
        String userName = authService.createNewUser(authRequest);
        authService.login(authRequest, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(userName));
    }

    @PostMapping("/sign-in")
    @Operation(summary = "Authenticates the user",
            description = "Accepts username and password, checks the presence of the user in the database. " +
                          "If present, creates a session and cookie")
    @ApiResponse(responseCode = "200", description = "Successful authentication")
    @ApiResponse(responseCode = "400", description = "Validation errors")
    @ApiResponse(responseCode = "401", description = "Incorrect data (there is no such user, or the password is " +
                                                     "incorrect")
    @ApiResponse(responseCode = "500", description = "Unknown error")
    public ResponseEntity<AuthResponse> auth(@RequestBody @Valid AuthRequest authRequest, HttpServletRequest request){
        authService.login(authRequest, request);
        return ResponseEntity.status(HttpStatus.OK).body(new AuthResponse(authRequest.username()));
    }
}
