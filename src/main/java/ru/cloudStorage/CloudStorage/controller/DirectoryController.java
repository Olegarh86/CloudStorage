package ru.cloudStorage.CloudStorage.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import ru.cloudStorage.CloudStorage.dto.RequestDto;
import ru.cloudStorage.CloudStorage.dto.ResourceResponseDto;
import ru.cloudStorage.CloudStorage.service.MinIOService;
import ru.cloudStorage.CloudStorage.util.PathCreator;

import java.util.List;


@RestController
@RequestMapping("/api/directory")
@Tag(name = "Folders", description = "Methods for working with folders")
public class DirectoryController {
    private final MinIOService minioService;
    private final PathCreator pathCreator;

    @Autowired
    public DirectoryController(MinIOService minioService, PathCreator pathCreator) {
        this.minioService = minioService;
        this.pathCreator = pathCreator;
    }

    @GetMapping
    @Operation(summary = "Getting information about the contents of a folder",
            description = "Returned collection of resources located in a folder (not recursive)")
    @ApiResponse(responseCode = "200", description = "A collection of resources located in a folder (not recursive)")
    @ApiResponse(responseCode = "400", description = "Invalid or missing path")
    @ApiResponse(responseCode = "401", description = "User is not authorized")
    @ApiResponse(responseCode = "404", description = "The folder not exists")
    @ApiResponse(responseCode = "500", description = "Unknown error")
    public ResponseEntity<List<ResourceResponseDto>> getDirectory(@AuthenticationPrincipal UserDetails userDetails,
                                                                  @Parameter(description = "Full path to the resource")
                                                                  @RequestParam("path") String path) {

        RequestDto dto = pathCreator.createRequestDto(userDetails.getUsername(), path);
        List<ResourceResponseDto> allObjects = minioService.getAllObjects(dto);
        return new ResponseEntity<>(allObjects, HttpStatus.OK);
    }

    @PostMapping
    @Operation(summary = "Create new empty folder",
            description = "Returned resource of the created folder")
    @ApiResponse(responseCode = "201", description = "Resource of the created folder")
    @ApiResponse(responseCode = "400", description = "Invalid or missing path")
    @ApiResponse(responseCode = "401", description = "User is not authorized")
    @ApiResponse(responseCode = "404", description = "The parent folder not exists")
    @ApiResponse(responseCode = "500", description = "Unknown error")
    public ResponseEntity<ResourceResponseDto> postDirectory(@AuthenticationPrincipal UserDetails userDetails,
                                                             @Parameter(description = "Full path to the resource")
                                                             @RequestParam("path") String path) {

        RequestDto dto = pathCreator.createRequestDto(userDetails.getUsername(), path);
        return new ResponseEntity<>(minioService.createNewFolder(dto), HttpStatus.CREATED);
    }
}
