package ru.cloudStorage.CloudStorage.controller;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.cloudStorage.CloudStorage.api.ResourcesApi;
import ru.cloudStorage.CloudStorage.dto.RenameRequestDto;
import ru.cloudStorage.CloudStorage.dto.RequestDto;
import ru.cloudStorage.CloudStorage.dto.ResourceResponseDto;
import ru.cloudStorage.CloudStorage.service.Storage;
import ru.cloudStorage.CloudStorage.util.PathCreator;

import java.util.List;

@RestController
public class ResourceController implements ResourcesApi {
    private final Storage storage;
    private final PathCreator pathCreator;

    public ResourceController(Storage storage, PathCreator pathCreator) {
        this.storage = storage;
        this.pathCreator = pathCreator;
    }

    @Override
    public ResponseEntity<Void> deleteObject(String path) {
        RequestDto dto = pathCreator.createRequestDto(path);
        storage.delete(dto);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    public ResponseEntity<Resource> downloadObject(String path) {
        RequestDto dto = pathCreator.createRequestDto(path);
        InputStreamResource resource = storage.download(dto);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(resource);
    }

    @Override
    public ResponseEntity<ResourceResponseDto> getObject(String path) {
        RequestDto dto = pathCreator.createRequestDto(path);
        ResourceResponseDto resourceResponseDto = storage.getObjectByPathWithName(dto);
        return new ResponseEntity<>(resourceResponseDto, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<ResourceResponseDto> moveOrRenameObject(String from, String to) {
        RenameRequestDto dto = pathCreator.createRequestRenameDto(from, to);
        ResourceResponseDto resourceResponseDto = storage.rename(dto);
        return new ResponseEntity<>(resourceResponseDto, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<ResourceResponseDto>> searchObject(String query) {
        RequestDto dto = pathCreator.createRequestDto(query);

        return new ResponseEntity<>(
                storage.search(dto),
                HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<ResourceResponseDto>> uploadObject(String path, List<MultipartFile> _object) {
        RequestDto requestDto = pathCreator.createRequestDto(path);
        List<ResourceResponseDto> upload = storage.upload(requestDto, _object);
        return new ResponseEntity<>(upload, HttpStatus.CREATED);
    }
}
