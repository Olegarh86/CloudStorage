package ru.cloudStorage.CloudStorage.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.cloudStorage.CloudStorage.dto.AuthResponse;

@RestController
@RequestMapping("/api/user")
@Tag(name = "User", description = "Information about authenticated user")
public class UserController {

    @GetMapping("/me")
    @Operation(summary = "Getting information about authenticated user",
            description = "Returned username")
    @ApiResponse(responseCode = "200", description = "Username")
    @ApiResponse(responseCode = "401", description = "User is not authorized")
    @ApiResponse(responseCode = "500", description = "Unknown error")
    public ResponseEntity<AuthResponse> auth(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.OK).body(new AuthResponse(userDetails.getUsername()));
    }
}
