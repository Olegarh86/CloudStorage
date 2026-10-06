package ru.cloudStorage.CloudStorage.service;

import io.minio.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import ru.cloudStorage.CloudStorage.dto.*;
import ru.cloudStorage.CloudStorage.exception.*;
import ru.cloudStorage.CloudStorage.exception.ObjectStreamException;
import ru.cloudStorage.CloudStorage.util.PathCreator;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class MinIOService {
    private final MinioClient minioClient;
    private final PathCreator pathCreator;
    @Value("${spring.servlet.multipart.max-file-size:100MB}")
    private DataSize maxFileSize;

    @Autowired
    public MinIOService(MinioClient minioClient, PathCreator pathCreator) {
        this.minioClient = minioClient;
        this.pathCreator = pathCreator;
    }

    public void createNewRootFolder(Long id) {
        RequestDto requestDto = pathCreator.createRootPath(id);
        createFolder(requestDto.bucketName(), requestDto.rootPath());
    }

    public List<ResourceResponseDto> getAllObjects(RequestDto requestDto) {
        List<ResourceResponseDto> result = new ArrayList<>();

        if (!objectAlreadyExist(requestDto.bucketName(), requestDto.rootPath(), requestDto.decodedPath())) {
            throw new ObjectNotExistException("Root folder not exist: " + requestDto.decodedPath());
        }
        Iterable<Result<Item>> objects = getListObjects(requestDto.bucketName(),
                requestDto.rootPath() + requestDto.decodedPath(), false);

        for (Result<Item> object : objects) {
            try {
                String nameWithFullPath = object.get()
                        .objectName()
                        .replaceFirst(requestDto.rootPath(), "");

                if (!nameWithFullPath.equals(requestDto.decodedPath()) && !nameWithFullPath.isBlank()) {
                    String path = pathCreator.getObjectPath(nameWithFullPath);
                    String name = pathCreator.getObjectName(nameWithFullPath);

                    if (object.get().isDir()) {
                        result.add(new ResourceResponseDto(path, name));
                    } else {
                        result.add(new ResourceResponseDto(path, name, object.get().size()));
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    public ResourceResponseDto createNewFolder(RequestDto requestDto) {
        FolderDto folderDto = pathCreator.createFolderDto(requestDto);

        if (!folderDto.name().endsWith("/")) {
            throw new ValidateException("Folder name must end with '/'");
        }

        if (!objectAlreadyExist(requestDto.bucketName(), requestDto.rootPath(), folderDto.pathWithoutName())) {
            throw new ObjectNotExistException("Root folder not exist: " + folderDto.pathWithoutName());
        }

        if (objectAlreadyExist(requestDto.bucketName(), requestDto.rootPath(), requestDto.decodedPath())) {
            throw new AlreadyExistException("Folder with name '" + requestDto.decodedPath() + "' already exists");
        }
        createFolder(folderDto.bucketName(), folderDto.fullPath());
        return new ResourceResponseDto(folderDto.pathWithoutName(), folderDto.name());
    }

    public ResourceResponseDto get(RequestDto requestDto) {
        String nameWithPath = requestDto.decodedPath();
        String path = pathCreator.getObjectPath(nameWithPath);
        String name = pathCreator.getObjectName(nameWithPath);

        StatObjectResponse objectStat =
                getStatObjectIfExist(requestDto.bucketName(), requestDto.rootPath(), requestDto.decodedPath());
        ResourceResponseDto result;

        if (objectStat.size() > 0) {
            result = new ResourceResponseDto(path, name, objectStat.size());
        } else {
            result = new ResourceResponseDto(path, name);
        }
        return result;
    }

    public void delete(RequestDto requestDto) {
        if (objectAlreadyExist(requestDto.bucketName(), requestDto.rootPath(), requestDto.decodedPath())) {
            Iterable<Result<Item>> deletedObjects = getListObjects(
                    requestDto.bucketName(), requestDto.rootPath() + requestDto.decodedPath(), true);

            removeObjects(requestDto.bucketName(), deletedObjects);
        } else {
            throw new ObjectNotExistException(requestDto.decodedPath());
        }
    }

    public List<ResourceResponseDto> upload(RequestDto requestDto, List<MultipartFile> files) {
        if (!objectAlreadyExist(requestDto.bucketName(), requestDto.rootPath(), requestDto.decodedPath())) {
            throw new NotFoundException("Resource not found: " + requestDto.decodedPath());
        }
        List<ResourceResponseDto> result = new ArrayList<>();

        for (MultipartFile file : files) {
            long maxBytes = maxFileSize.toBytes();
            long objectSize = file.getSize();

            if (objectSize > maxBytes) {
                throw new FileTooLargeException("TooLarge.file");
            }
            String path = requestDto.decodedPath();
            String objectName = file.getOriginalFilename();

            assert objectName != null;
            if (objectName.contains("/")) {
                createSubfolders(requestDto, path, objectName);
            }
            objectName = duplicateNameRemover(requestDto, objectName);

            if (objectSize > 0 && objectName != null && !objectName.isBlank()) {

                try (InputStream is = file.getInputStream()) {
                    uploadObject(requestDto.bucketName(), requestDto.rootPath() + path + objectName,
                            new UploadDto(is, objectSize, -1, file.getContentType()));
                    result.add(new ResourceResponseDto(path, objectName, objectSize));
                } catch (IOException e) {
                    throw new UploadObjectException("Error loading file into MinIO: " + objectName, e);
                }
            } else {
                createFolder(requestDto.bucketName(), requestDto.rootPath() + path + objectName);
                result.add(new ResourceResponseDto(path, objectName));
            }
        }
        return result;
    }

    public InputStreamResource download(RequestDto requestDto) {
        if (!objectAlreadyExist(requestDto.bucketName(), requestDto.rootPath(), requestDto.decodedPath())) {
            throw new ObjectNotExistException("Object not exist: " + requestDto.decodedPath());
        }
        String fullPath = requestDto.rootPath() + requestDto.decodedPath();

        try {
            PipedInputStream pipedInputStream = new PipedInputStream(32768);
            PipedOutputStream pipedOutputStream = new PipedOutputStream(pipedInputStream);

            new Thread(() -> {
                try (pipedOutputStream) {

                    if (requestDto.decodedPath().endsWith("/")) {
                        try (ZipOutputStream zos = new ZipOutputStream(pipedOutputStream, StandardCharsets.UTF_8)) {
                            Iterable<Result<Item>> listObjects = getListObjects(requestDto.bucketName(), fullPath, true);
                            byte[] buffer = new byte[32768];

                            for (Result<Item> object : listObjects) {

                                if (!object.get().isDir()) {
                                    String name = object.get().objectName();
                                    zos.putNextEntry(new ZipEntry(name.substring(fullPath.length())));
                                    try (InputStream is = getObjectStream(requestDto.bucketName(), name)) {
                                        int bytesRead;

                                        while ((bytesRead = is.read(buffer)) != -1) {
                                            zos.write(buffer, 0, bytesRead);
                                        }
                                    }
                                    zos.closeEntry();
                                }
                            }
                            zos.finish();
                        }
                    } else {
                        try (InputStream is = getObjectStream(requestDto.bucketName(), fullPath)) {
                            is.transferTo(pipedOutputStream);
                        }
                    }
                } catch (Exception e) {
                    throw new ObjectStreamException("Error while streaming data from MinIO", e);
                }
            }).start();
            return new InputStreamResource(pipedInputStream);
        } catch (IOException e) {
            throw new ObjectStreamException("Failed to initialize piped streams", e);
        }
    }

    public ResourceResponseDto rename(RenameRequestDto requestDto) {
        if (!objectAlreadyExist(requestDto.bucketName(), requestDto.rootPath(), requestDto.decodedPathFrom())) {
            throw new NotFoundException("Resource not found: '" + requestDto.decodedPathFrom() + "'");
        }

        if (objectAlreadyExist(requestDto.bucketName(), requestDto.rootPath(), requestDto.decodedPathTo())) {
            throw new AlreadyExistException("Resource already exist: '" + requestDto.decodedPathTo() + "'");
        }
        ResourceResponseDto responseDto;

        if (requestDto.decodedPathFrom().endsWith("/")) {
            Iterable<Result<Item>> objects = getListObjects(
                    requestDto.bucketName(), requestDto.rootPath() + requestDto.decodedPathFrom(), true);

            for (Result<Item> object : objects) {
                Item item;
                try {
                    item = object.get();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                String oldNameWithFullPath = item.objectName();
                RenameRequestDto newRequestDto = getRenameRequestDto(requestDto, oldNameWithFullPath);

                if (oldNameWithFullPath.endsWith("/")) {
                    createFolder(requestDto.bucketName(), requestDto.rootPath() + newRequestDto.decodedPathTo());
                    deleteObject(requestDto.bucketName(), requestDto.rootPath(), newRequestDto.decodedPathFrom());
                } else {
                    renameObject(newRequestDto);
                }
            }
            createFolder(requestDto.bucketName(), requestDto.rootPath() + requestDto.decodedPathTo());
            deleteObject(requestDto.bucketName(), requestDto.rootPath(), requestDto.decodedPathFrom());
            responseDto = new ResourceResponseDto(
                    pathCreator.getObjectPath(requestDto.decodedPathTo()),
                    pathCreator.getObjectName(requestDto.decodedPathTo()));
        } else {
            responseDto = renameObject(requestDto);
        }
        return responseDto;

    }

    public boolean objectAlreadyExist(String bucketName, String rootPath, String decodedPath) {
        try {
            getStatObjectIfExist(bucketName, rootPath, decodedPath);
        } catch (ObjectNotExistException e) {
            return false;
        }
        return true;
    }

    public List<ResourceResponseDto> search(RequestDto requestDto) {
        List<ResourceResponseDto> result = new ArrayList<>();
        Iterable<Result<Item>> objects = getListObjects(requestDto.bucketName(), requestDto.rootPath(), true);

        for (Result<Item> object : objects) {
            String objectNameWithFullPath;
            try {
                objectNameWithFullPath = object.get().objectName();
            } catch (Exception e) {
                throw new NotFoundException("Not found resource: " + requestDto.decodedPath());
            }
            String fullPath = pathCreator.getObjectPath(objectNameWithFullPath);
            String name = pathCreator.getObjectName(objectNameWithFullPath);

            if (name.toLowerCase().contains(requestDto.decodedPath().toLowerCase())) {
                String path = fullPath.substring(requestDto.rootPath().length());
                ResourceResponseDto responseDto;

                if (objectNameWithFullPath.endsWith("/") && !objectNameWithFullPath.equals(requestDto.rootPath())) {
                    responseDto = new ResourceResponseDto(path, name);
                } else {
                    StatObjectResponse objectStat = getStatObjectIfExist(requestDto.bucketName(),
                            requestDto.rootPath(), path + name);
                    responseDto = new ResourceResponseDto(path, name, objectStat.size());
                }
                result.add(responseDto);
            }
        }
        return result;
    }

    private String renameDuplicate(String name) {
        if (name.contains(".")) {
            int dotIndex = name.lastIndexOf(".");
            String firstPartName = name.substring(0, dotIndex);
            firstPartName += 1;
            return firstPartName + name.substring(dotIndex);
        }
        return name + 1;
    }

    private String duplicateNameRemover(RequestDto requestDto, String objectName) {
        if (objectAlreadyExist(
                requestDto.bucketName(), requestDto.rootPath(), requestDto.decodedPath() + objectName)) {
            String finalName = renameDuplicate(objectName);
            return duplicateNameRemover(requestDto, finalName);
        } else {
            return objectName;
        }
    }

    private void createSubfolders(RequestDto requestDto, String objectPath, String name) {
        int lastSlash = name.lastIndexOf("/");
        String folderName = name.substring(0, lastSlash);

        if (!objectAlreadyExist(requestDto.bucketName(), requestDto.rootPath(), objectPath + folderName)) {
            createFolder(requestDto.bucketName(), requestDto.rootPath() + objectPath + folderName + "/");
        }

        if (folderName.contains("/")) {
            createSubfolders(requestDto, objectPath, folderName);
        }
    }

    private ResourceResponseDto renameObject(RenameRequestDto requestDto) {
        copyObject(requestDto.bucketName(), requestDto.rootPath(), requestDto.decodedPathFrom(),
                requestDto.decodedPathTo());
        deleteObject(requestDto.bucketName(), requestDto.rootPath(), requestDto.decodedPathFrom());
        StatObjectResponse object = getStatObjectIfExist(requestDto.bucketName(), requestDto.rootPath(), requestDto.decodedPathTo());
        return new ResourceResponseDto(
                pathCreator.getObjectPath(requestDto.decodedPathTo()),
                pathCreator.getObjectName(requestDto.decodedPathTo()),
                object.size());
    }

    @NotNull
    private RenameRequestDto getRenameRequestDto(RenameRequestDto requestDto, String oldNameWithFullPath) {
        String newNameWithFullPath = oldNameWithFullPath.replace(requestDto.decodedPathFrom(), requestDto.decodedPathTo());

        String oldNameWithPath = oldNameWithFullPath.replace(requestDto.rootPath(), "");
        String newNameWithPath = newNameWithFullPath.replace(requestDto.rootPath(), "");

        return new RenameRequestDto(requestDto.bucketName(), requestDto.rootPath(), oldNameWithPath, newNameWithPath);
    }

    private void createFolder(String bucketName, String objectName) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(new ByteArrayInputStream(new byte[]{}), 0, -1)
                            .build());
        } catch (Exception e) {
            throw new CreateNewFolderException("Folder not created: " + objectName, e);
        }
    }

    private InputStream getObjectStream(String bucketName, String objectName) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build());
        } catch (Exception e) {
            throw new ObjectStreamException("Failed to get object stream: " + objectName, e);
        }
    }

    private Iterable<Result<Item>> getListObjects(String bucketName, String objectName, boolean recursive) {
        return minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(objectName)
                        .recursive(recursive)
                        .build());
    }

    private StatObjectResponse getStatObjectIfExist(String bucketName, String rootPath, String objectName) {
        try {
            return minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(rootPath + objectName)
                            .build());
        } catch (Exception e) {
            throw new ObjectNotExistException("Object not exist: " + objectName, e);
        }
    }

    private void uploadObject(String bucketName, String objectName, UploadDto uploadDto) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(uploadDto.inputStream(), uploadDto.objectSize(), uploadDto.partSize())
                            .contentType(uploadDto.contentType())
                            .build());
        } catch (Exception e) {
            throw new UploadObjectException("Failed to load object: " + objectName, e);
        }
    }

    private void copyObject(String bucketName, String rootPath, String pathFrom, String pathTo) {
        try {
            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(bucketName)
                            .object(rootPath + pathTo)
                            .source(CopySource.builder()
                                    .bucket(bucketName)
                                    .object(rootPath + pathFrom)
                                    .build())
                            .build());

        } catch (Exception e) {
            throw new CopyObjectException("Object not copied: " + pathFrom, e);
        }
    }

    private void deleteObject(String bucketName, String rootPath, String pathFrom) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(rootPath + pathFrom)
                            .build());
        } catch (Exception e) {
            throw new RemoveObjectException("Object not deleted: " + pathFrom, e);
        }
    }

    private void removeObjects(String bucketName, Iterable<Result<Item>> deletedObjects) {
        List<DeleteObject> objects = new LinkedList<>();

        for (Result<Item> result : deletedObjects) {
            try {
                objects.add(new DeleteObject(result.get().objectName()));
            } catch (Exception e) {
                throw new RemoveObjectException("Object not deleted", e);
            }
        }
        Iterable<Result<DeleteError>> results = minioClient.removeObjects(
                RemoveObjectsArgs.builder()
                        .bucket(bucketName)
                        .objects(objects)
                        .build());

        for (Result<DeleteError> result : results) {
            try {
                DeleteError error = result.get();
                System.out.println(
                        "Error in deleting object " + error.objectName() + "; " + error.message());
            } catch (Exception e) {
                throw new RemoveObjectException("Object not deleted", e);
            }
        }
    }
}
