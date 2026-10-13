package ru.cloudStorage.CloudStorage.service;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.cloudStorage.CloudStorage.dto.*;
import ru.cloudStorage.CloudStorage.exception.*;
import ru.cloudStorage.CloudStorage.exception.ObjectStreamException;
import ru.cloudStorage.CloudStorage.util.PathCreator;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static ru.cloudStorage.CloudStorage.dto.ResourceType.DIRECTORY;
import static ru.cloudStorage.CloudStorage.dto.ResourceType.FILE;

@Service
public class MinIOService {
    private final MinioClient minioClient;
    private final PathCreator pathCreator;

    @Autowired
    public MinIOService(MinioClient minioClient, PathCreator pathCreator) {
        this.minioClient = minioClient;
        this.pathCreator = pathCreator;
    }

    public void createNewRootFolder(Long id) {
        RequestDto requestDto = pathCreator.createRootPath(id);
        createFolder(requestDto.bucketName(), requestDto.rootPath(), "");
    }

    public List<ResourceResponseDto> getAllObjectsFromDirectory(RequestDto requestDto) {
        if (!objectAlreadyExist(requestDto.bucketName(), requestDto.rootPath(), requestDto.decodedPath())) {
            throw new ObjectNotExistException("Root folder not exist: " + requestDto.decodedPath());
        }
        List<StorageObject> objects = getAllObjects(requestDto.bucketName(),
                requestDto.rootPath(), requestDto.decodedPath(), false);
        List<ResourceResponseDto> result = new ArrayList<>();

        for (StorageObject object : objects) {
            String namePathWithoutRoot = object.pathWithName().replace(requestDto.rootPath(), "");

            if (!namePathWithoutRoot.equals(requestDto.decodedPath()) && !namePathWithoutRoot.isBlank()) {
                String path = pathCreator.getObjectPath(namePathWithoutRoot);
                String name = pathCreator.getObjectName(namePathWithoutRoot);

                if (object instanceof StorageFile file) {
                    result.add(new ResourceResponseDto(path, name, file.size()));
                } else {
                    result.add(new ResourceResponseDto(path, name));
                }
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
        createFolder(folderDto.bucketName(), folderDto.rootPath(), folderDto.pathWithoutName() + folderDto.name());
        return new ResourceResponseDto(folderDto.pathWithoutName(), folderDto.name());
    }

    public ResourceResponseDto getObjectByPathWithName(RequestDto requestDto) {
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
            deleteObjects(requestDto.bucketName(), requestDto.rootPath(), requestDto.decodedPath());
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
            long objectSize = file.getSize();

            String path = requestDto.decodedPath();
            String objectName = file.getOriginalFilename();

            if (objectName == null) {
                throw new ValidateException("File name is empty");
            }
            pathCreator.validatePath(objectName);

            if (objectName.contains("/")) {
                createSubfolders(requestDto, path, objectName);
            }
            objectName = duplicateNameRemover(requestDto, objectName);

            if (objectSize > 0 && objectName != null && !objectName.isBlank()) {

                try (InputStream is = file.getInputStream()) {
                    uploadObject(requestDto.bucketName(), requestDto.rootPath() + path + objectName,
                            new UploadDto(is, objectSize, -1, file.getContentType()));
                    result.add(new ResourceResponseDto(path, objectName, objectSize));
                } catch (Exception e) {
                    throw new UploadObjectException("Error loading file into MinIO: " + objectName, e);
                }
            } else {
                createFolder(requestDto.bucketName(), requestDto.rootPath(), path + objectName);
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
            int piece = 32768;
            PipedInputStream pipedInputStream = new PipedInputStream(piece);
            PipedOutputStream pipedOutputStream = new PipedOutputStream(pipedInputStream);

            new Thread(() -> {
                try (pipedOutputStream) {

                    if (requestDto.decodedPath().endsWith("/")) {
                        try (ZipOutputStream zos = new ZipOutputStream(pipedOutputStream, StandardCharsets.UTF_8)) {
                            List<StorageObject> objects = getAllObjects(requestDto.bucketName(),
                                    requestDto.rootPath(), requestDto.decodedPath(), true);
                            byte[] buffer = new byte[piece];

                            for (StorageObject object : objects) {

                                if (!object.isDirectory()) {
                                    String name = object.pathWithName();
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

    public ResourceResponseDto rename(RenameRequestDto renameRequestDto) {
        if (objectAlreadyExist(renameRequestDto.bucketName(), renameRequestDto.rootPath(), renameRequestDto.pathWithNameTo())) {
            throw new AlreadyExistException("Resource already exist: '" + renameRequestDto.pathWithNameTo() + "'");
        }
        ResourceResponseDto responseDto;
        List<RenameRequestDto> tempObjects = new ArrayList<>();

        try {
            if (renameRequestDto.pathWithNameFrom().endsWith("/")) {
                List<String> pathsWithNames = getFullPathsWithNames(renameRequestDto.bucketName(),
                        renameRequestDto.rootPath(), renameRequestDto.pathWithNameFrom());

                for (String pathWithName : pathsWithNames) {
                    RenameRequestDto newRequestDto = getRenameRequestDto(renameRequestDto, pathWithName);

                    if (pathWithName.endsWith("/")) {
                        createFolder(newRequestDto.bucketName(), newRequestDto.rootPath(), newRequestDto.pathWithNameTo());
                    } else {
                        copyObject(newRequestDto.bucketName(), newRequestDto.rootPath(), newRequestDto.pathWithNameFrom(),
                                newRequestDto.pathWithNameTo());
                    }
                    tempObjects.add(newRequestDto);
                }
                try {
                    deleteObjects(renameRequestDto.bucketName(),
                            renameRequestDto.rootPath(), renameRequestDto.pathWithNameFrom());
                } catch (Exception e) {
                    throw new RenameObjectException("Error while deleted old data from MinIO", e);
                }
                responseDto = new ResourceResponseDto(
                        pathCreator.getObjectPath(renameRequestDto.pathWithNameTo()),
                        pathCreator.getObjectName(renameRequestDto.pathWithNameTo()));
            } else {
                copyObject(renameRequestDto.bucketName(), renameRequestDto.rootPath(), renameRequestDto.pathWithNameFrom(),
                        renameRequestDto.pathWithNameTo());
                tempObjects.add(renameRequestDto);
                StatObjectResponse object = getStatObjectIfExist(renameRequestDto.bucketName(),
                        renameRequestDto.rootPath(), renameRequestDto.pathWithNameTo());
                try {
                    deleteObject(renameRequestDto.bucketName(), renameRequestDto.rootPath(), renameRequestDto.pathWithNameFrom());
                } catch (Exception e) {
                    throw new RenameObjectException("Error while deleted old data from MinIO", e);
                }
                responseDto = new ResourceResponseDto(pathCreator.getObjectPath(renameRequestDto.pathWithNameTo()),
                        pathCreator.getObjectName(renameRequestDto.pathWithNameTo()),
                        object.size());
            }
        } catch (ObjectNotExistException e) {
            throw new NotFoundException("Object not exist: " + renameRequestDto.pathWithNameFrom());
        } catch (RenameObjectException e) {
            throw new RenameObjectException("Error while renaming data from MinIO", e);
        } catch (Exception e) {
            for (RenameRequestDto object : tempObjects) {
                deleteObject(object.bucketName(), object.rootPath(), object.pathWithNameTo());
            }
            throw new RenameObjectException("Error while renaming data from MinIO", e);
        }
        return responseDto;
    }

    private List<StorageObject> getAllObjects(String bucketName, String rootPath, String pathWithName,
                                              boolean recursive) {
        Iterable<Result<Item>> objects = getListObjects(bucketName,rootPath + pathWithName, recursive);
        String nameWithFullPath;
        List<StorageObject> result = new ArrayList<>();

        for (Result<Item> object : objects) {
            try {
                Item item = object.get();
                nameWithFullPath = item.objectName();

                if (item.isDir()) {
                    result.add(new StorageDirectory(DIRECTORY, nameWithFullPath));
                } else {
                    result.add(new StorageFile(FILE, nameWithFullPath, item.size()));
                }
            } catch (Exception e) {
                throw new MinIOException("Error when getting object from MinIO", e);
            }
        }
        return result;
    }

    private List<String> getFullPathsWithNames(String bucketName, String rootPath, String pathWithName) {
        List<String> result = new ArrayList<>();
        Iterable<Result<Item>> objects = getListObjects(bucketName, rootPath + pathWithName, true);

        if (objects.iterator().hasNext()) {
            for (Result<Item> object : objects) {
                Item item;
                try {
                    item = object.get();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                result.add(item.objectName());
            }
        } else {
            throw new NotFoundException("Resource not found: '" + pathWithName + "'");
        }
        return result;
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
        List<String> pathsWithNames = getFullPathsWithNames(requestDto.bucketName(), requestDto.rootPath(), "");
        List<ResourceResponseDto> result = new ArrayList<>();

        for (String pathWithName : pathsWithNames) {
            String fullPath = pathCreator.getObjectPath(pathWithName);
            String name = pathCreator.getObjectName(pathWithName);

            if (name.toLowerCase().contains(requestDto.decodedPath().toLowerCase()) && !fullPath.isBlank()) {
                String path = fullPath.substring(requestDto.rootPath().length());
                ResourceResponseDto responseDto;

                if (pathWithName.endsWith("/") && !pathWithName.equals(requestDto.rootPath())) {
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

        if (!objectAlreadyExist(requestDto.bucketName(), requestDto.rootPath(), objectPath + folderName + "/")) {
            createFolder(requestDto.bucketName(), requestDto.rootPath(), objectPath + folderName + "/");
        }

        if (folderName.contains("/")) {
            createSubfolders(requestDto, objectPath, folderName);
        }
    }

    @NotNull
    private RenameRequestDto getRenameRequestDto(RenameRequestDto requestDto, String oldNameWithFullPath) {
        String newNameWithFullPath = oldNameWithFullPath.replace(requestDto.pathWithNameFrom(), requestDto.pathWithNameTo());

        String oldNameWithPath = oldNameWithFullPath.replace(requestDto.rootPath(), "");
        String newNameWithPath = newNameWithFullPath.replace(requestDto.rootPath(), "");

        return new RenameRequestDto(requestDto.bucketName(), requestDto.rootPath(), oldNameWithPath, newNameWithPath);
    }

    private void createFolder(String bucketName, String rootPath, String objectName) {
        try {
            Map<String, String> headers = Map.of("If-None-Match", "*");
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(rootPath + objectName)
                            .stream(new ByteArrayInputStream(new byte[]{}), 0, -1)
                            .headers(headers)
                            .build());
        } catch (ErrorResponseException e) {
            throw translateException(e, objectName);
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
        } catch (ErrorResponseException e) {
            throw translateException(e, objectName);
        } catch (Exception e) {
            throw new ObjectStreamException("Failed to get object stream: " + objectName, e);
        }
    }

    private StatObjectResponse getStatObjectIfExist(String bucketName, String rootPath, String objectName) {
        try {
            return minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(rootPath + objectName)
                            .build());
        } catch (ErrorResponseException e) {
            throw translateException(e, objectName);
        } catch (Exception e) {
            throw new GetStatObjectException("Exception while getting stat object: " + objectName, e);
        }
    }

    private void uploadObject(String bucketName, String objectName, UploadDto uploadDto) {
        try {
            Map<String, String> headers = Map.of("If-None-Match", "*");
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(uploadDto.inputStream(), uploadDto.objectSize(), uploadDto.partSize())
                            .contentType(uploadDto.contentType())
                            .headers(headers)
                            .build());
        } catch (ErrorResponseException e) {
            throw translateException(e, objectName);
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
        } catch (ErrorResponseException e) {
            throw translateException(e, pathFrom);
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
        } catch (ErrorResponseException e) {
            throw translateException(e, pathFrom);
        } catch (Exception e) {
            throw new RemoveObjectException("Object not deleted: " + pathFrom, e);
        }
    }

    private void deleteObjects(String bucketName, String rootPath, String pathWithName) {
        Iterable<Result<Item>> deletedObjects = getListObjects(bucketName, rootPath + pathWithName, true);
        List<DeleteObject> objects = new LinkedList<>();

        for (Result<Item> object : deletedObjects) {
            try {
                objects.add(new DeleteObject(object.get().objectName()));
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

    private Iterable<Result<Item>> getListObjects(String bucketName, String objectName, boolean recursive) {
        return minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(objectName)
                        .recursive(recursive)
                        .build());
    }

    private RuntimeException translateException(ErrorResponseException e, String objectName) {
        String errorCode = e.errorResponse().code();
        if ("NoSuchKey".equals(errorCode) || "NoSuchObject".equals(errorCode)) {
            return new ObjectNotExistException("Object not exist: " + objectName, e);
        }
        if ("PreconditionFailed".equals(errorCode)) {
            return new AlreadyExistException("Object with name '" + objectName + "' already exists");
        }
        return new MinIOException(objectName, e);
    }
}
