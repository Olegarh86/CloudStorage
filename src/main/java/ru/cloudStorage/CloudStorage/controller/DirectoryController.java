package ru.cloudStorage.CloudStorage.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.cloudStorage.CloudStorage.api.FoldersApi;
import ru.cloudStorage.CloudStorage.dto.RequestDto;
import ru.cloudStorage.CloudStorage.dto.ResourceResponseDto;
import ru.cloudStorage.CloudStorage.service.Storage;
import ru.cloudStorage.CloudStorage.util.PathCreator;

import java.util.List;


@RestController
public class DirectoryController implements FoldersApi {
    private final Storage storage;
    private final PathCreator pathCreator;

    @Autowired
    public DirectoryController(Storage storage, PathCreator pathCreator) {
        this.storage = storage;
        this.pathCreator = pathCreator;
    }

    @Override
    public ResponseEntity<List<ResourceResponseDto>> getDirectory(String path) {
        RequestDto dto = pathCreator.createRequestDto(path);
        List<ResourceResponseDto> allObjects = storage.getAllObjectsFromDirectory(dto);
        return new ResponseEntity<>(allObjects, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<ResourceResponseDto> postDirectory(String path) {
        RequestDto dto = pathCreator.createRequestDto(path);
        return new ResponseEntity<>(storage.createNewFolder(dto), HttpStatus.CREATED);
    }
}
