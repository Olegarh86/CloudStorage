package ru.cloudStorage.CloudStorage.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.cloudStorage.CloudStorage.dto.RenameRequestDto;
import ru.cloudStorage.CloudStorage.dto.RequestDto;
import ru.cloudStorage.CloudStorage.dto.ResourceResponseDto;
import ru.cloudStorage.CloudStorage.service.MinIOService;
import ru.cloudStorage.CloudStorage.util.PathCreator;

import java.util.List;

@RestController
@RequestMapping("/api/resource")
@Tag(name = "Resources", description = """
        Methods for working with resources. For all requests below, the path parameter
         is the full path to the resource in url-encoded format. The folder path must
          end with /. This is necessary to distinguish between a folder and a file with
           the same name that may coexist together in the same root directory
        """)
public class ResourceController {
    private final MinIOService minioService;
    private final PathCreator pathCreator;

    public ResourceController(MinIOService minioService, PathCreator pathCreator) {
        this.minioService = minioService;
        this.pathCreator = pathCreator;
    }

    @GetMapping
    @Operation(summary = "Getting information about a resource",
            description = "Returned information about the resource")
    @ApiResponse(responseCode = "200", description = "Information about the resource")
    @ApiResponse(responseCode = "400", description = "Invalid or missing path")
    @ApiResponse(responseCode = "401", description = "User is not authorized")
    @ApiResponse(responseCode = "404", description = "Ресурс не найден")
    @ApiResponse(responseCode = "500", description = "Unknown error")
    public ResponseEntity<ResourceResponseDto> getObject(@AuthenticationPrincipal UserDetails userDetails,
                                                         @Parameter(description = "Full path to the resource")
                                                         @RequestParam("path") String path) {
        RequestDto dto = pathCreator.createRequestDto(userDetails.getUsername(), path);
        ResourceResponseDto resourceResponseDto = minioService.get(dto);
        return new ResponseEntity<>(resourceResponseDto, HttpStatus.OK);
    }

    @DeleteMapping
    @Operation(summary = "Deleting a resource",
            description = "Accepts the path to the resource to be deleted")
    @ApiResponse(responseCode = "204", description = "Without a body")
    @ApiResponse(responseCode = "400", description = "Invalid or missing path")
    @ApiResponse(responseCode = "401", description = "User is not authorized")
    @ApiResponse(responseCode = "404", description = "Resource not found")
    @ApiResponse(responseCode = "500", description = "Unknown error")
    public ResponseEntity<Void> deleteObject(@AuthenticationPrincipal UserDetails userDetails,
                                             @Parameter(description = "Full path to the resource")
                                             @RequestParam("path") String path) {
        RequestDto dto = pathCreator.createRequestDto(userDetails.getUsername(), path);
        minioService.delete(dto);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload",
            description = """
                    GET parameter path - the path to the folder into which we download the resource(s).
                    The request body contains data from file input in MultipartFile format. If a subdirectory is
                    specified in the file name (for example, upload_folder/test.txt), then when uploading to
                    storage_folder/ such a directory will be created. As a result, the file after uploading will be
                    located in storage_folder/upload_folder/test.txt. This allows you to download files, folders and
                    recursively nested subfolders in a single request.
                    """)
    @ApiResponse(responseCode = "201", description = "Collection of downloaded resources")
    @ApiResponse(responseCode = "400", description = "Invalid request body")
    @ApiResponse(responseCode = "401", description = "User is not authorized")
    @ApiResponse(responseCode = "409", description = "The file already exists")
    @ApiResponse(responseCode = "500", description = "Unknown error")
    public ResponseEntity<List<ResourceResponseDto>> uploadObject(@AuthenticationPrincipal UserDetails userDetails,
                                                                  @Parameter(description = "File input in MultipartFile format")
                                                                  @RequestPart("object") MultipartFile[] files,
                                                                  @Parameter(description = "Full path to the resource")
                                                                  @RequestParam("path") String path) {
        RequestDto dto = pathCreator.createRequestDto(userDetails.getUsername(), path);
        List<ResourceResponseDto> upload = minioService.upload(dto, files);
        return new ResponseEntity<>(upload, HttpStatus.CREATED);
    }

    @GetMapping("/download")
    @Operation(summary = "Downloading a resource",
            description = "The folder is downloaded as a zip archive of its contents")
    @ApiResponse(responseCode = "200", description = "Binary content of a file with Content-Type: application/octet-stream")
    @ApiResponse(responseCode = "400", description = "Invalid or missing path")
    @ApiResponse(responseCode = "401", description = "User is not authorized")
    @ApiResponse(responseCode = "404", description = "Resource not found")
    @ApiResponse(responseCode = "500", description = "Unknown error")
    public ResponseEntity<InputStreamResource> downloadObject(@AuthenticationPrincipal UserDetails userDetails,
                                                              @Parameter(description = "Full path to the resource")
                                                              @RequestParam("path") String path) {
        RequestDto dto = pathCreator.createRequestDto(userDetails.getUsername(), path);
        InputStreamResource resource = new InputStreamResource(minioService.download(dto));

        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    @PostMapping("/move")
    @Operation(summary = "Renaming/moving a resource",
            description = "GET parameters - old and new full paths to the resource in URL-encoded format")
    @ApiResponse(responseCode = "200", description = "Information about the moved/renamed resource")
    @ApiResponse(responseCode = "400", description = "Invalid or missing path")
    @ApiResponse(responseCode = "401", description = "User is not authorized")
    @ApiResponse(responseCode = "404", description = "Resource not found")
    @ApiResponse(responseCode = "409", description = "The resource lying on the path to already exists")
    @ApiResponse(responseCode = "500", description = "Unknown error")
    public ResponseEntity<ResourceResponseDto> moveOrRenameObject(@AuthenticationPrincipal UserDetails userDetails,
                                                                  @Parameter(description = "Old path to the resource")
                                                                  @RequestParam("from") String from,
                                                                  @Parameter(description = "New path to the resource")
                                                                  @RequestParam("to") String to) {
        RenameRequestDto dto = pathCreator.createRequestRenameDto(userDetails.getUsername(), from, to);
        ResourceResponseDto resourceResponseDto = minioService.rename(dto);
        return new ResponseEntity<>(resourceResponseDto, HttpStatus.OK);
    }

    @GetMapping("/search")
    @Operation(summary = "Search",
            description = "GET parameter query - search query in URL-encoded format")
    @ApiResponse(responseCode = "200", description = "Collection of found resources")
    @ApiResponse(responseCode = "400", description = "Invalid or missing search query")
    @ApiResponse(responseCode = "401", description = "User is not authorized")
    @ApiResponse(responseCode = "500", description = "Unknown error")
    public ResponseEntity<List<ResourceResponseDto>> searchObject(@AuthenticationPrincipal UserDetails userDetails,
                                                                  @Parameter(description = "Search query")
                                                                  @RequestParam("query") String queryPath) {
        RequestDto dto = pathCreator.createRequestDto(userDetails.getUsername(), queryPath);

        return new ResponseEntity<>(
                minioService.search(dto),
                HttpStatus.OK);
    }
}
