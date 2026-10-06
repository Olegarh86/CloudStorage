package ru.cloudStorage.CloudStorage.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.cloudStorage.CloudStorage.api.FoldersApi;
import ru.cloudStorage.CloudStorage.dto.RequestDto;
import ru.cloudStorage.CloudStorage.dto.ResourceResponseDto;
import ru.cloudStorage.CloudStorage.service.MinIOService;
import ru.cloudStorage.CloudStorage.util.PathCreator;

import java.util.List;


@RestController
public class DirectoryController implements FoldersApi {
    private final MinIOService minioService;
    private final PathCreator pathCreator;

    @Autowired
    public DirectoryController(MinIOService minioService, PathCreator pathCreator) {
        this.minioService = minioService;
        this.pathCreator = pathCreator;
    }

    @Override
    public ResponseEntity<List<ResourceResponseDto>> getDirectory(String path) {
        RequestDto dto = pathCreator.createRequestDto(path);
        List<ResourceResponseDto> allObjects = minioService.getAllObjects(dto);
        return new ResponseEntity<>(allObjects, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<ResourceResponseDto> postDirectory(String path) {
        RequestDto dto = pathCreator.createRequestDto(path);
        return new ResponseEntity<>(minioService.createNewFolder(dto), HttpStatus.CREATED);
    }
}
