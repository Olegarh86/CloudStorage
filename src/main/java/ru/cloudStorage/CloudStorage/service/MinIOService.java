package ru.cloudStorage.CloudStorage.service;

import io.minio.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;
import ru.cloudStorage.CloudStorage.dto.*;
import ru.cloudStorage.CloudStorage.exception.*;
import ru.cloudStorage.CloudStorage.util.PathCreator;

import java.io.*;
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

    public List<ResourceResponseDto> getAllObjects(RequestDto requestDto) {
//        checkFolderName(pathCreator.getObjectName(requestDto.decodedPath()));
        List<ResourceResponseDto> answer = new ArrayList<>();
        Iterable<Result<Item>> results = getListObjects(requestDto.bucketName(),
                requestDto.rootPath() + requestDto.decodedPath(), false);

        if (!results.iterator().hasNext()) {
            throw new ObjectNotExistException(requestDto.decodedPath());
        }

        for (Result<Item> result : results) {
            try {
                String objectPathWithName = result.get()
                        .objectName()
                        .replaceFirst(requestDto.rootPath(), "");
                String objectPath;
                String objectName;

                if (!objectPathWithName.equals(requestDto.decodedPath()) && !objectPathWithName.isBlank()) {
                    objectPath = pathCreator.getObjectPath(objectPathWithName);
                    objectName = pathCreator.getObjectName(objectPathWithName);

                    if (result.get().isDir()) {
                        answer.add(new ResourceResponseDto(objectPath, objectName));
                    } else {
                        answer.add(new ResourceResponseDto(objectPath, objectName, result.get().size()));
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return answer;
    }

    public ResourceResponseDto createNewFolder(RequestDto requestDto) {
        FolderDto folderDto = pathCreator.createFolderDto(requestDto);
//        checkFolderName(folderDto.name());
        Iterable<Result<Item>> results = getListObjects(requestDto.bucketName(),
                requestDto.rootPath() + folderDto.pathWithoutName(), false);

        if (!results.iterator().hasNext()) {
            throw new ObjectNotExistException(folderDto.pathWithoutName());
        }
        createFolder(folderDto.bucketName(), folderDto.fullPath());
        return new ResourceResponseDto(folderDto.pathWithoutName(), folderDto.name());
    }

    public ResourceResponseDto get(RequestDto requestDto) {
        String objectPathWithName = requestDto.decodedPath();
        String objectPath = pathCreator.getObjectPath(objectPathWithName);
        String objectName = pathCreator.getObjectName(objectPathWithName);
        ResourceResponseDto answer;

        StatObjectResponse objectStat =
                getStatObject(requestDto.bucketName(), requestDto.rootPath(), requestDto.decodedPath());

        if (objectStat.size() > 0) {
            answer = new ResourceResponseDto(objectPath, objectName, objectStat.size());
        } else {
            answer = new ResourceResponseDto(objectPath, objectName);
        }
        return answer;
    }

    public void delete(RequestDto requestDto) {

        if (objectAlreadyExist(requestDto.bucketName(), requestDto.rootPath(), requestDto.decodedPath())) {
            Iterable<Result<Item>> deletedObjects = getListObjects(
                    requestDto.bucketName(), requestDto.rootPath() + requestDto.decodedPath(), true);

            removeObjects(requestDto.bucketName(), deletedObjects);
        }
    }

    public List<ResourceResponseDto> upload(RequestDto requestDto, MultipartFile[] files) {

        if (objectAlreadyExist(requestDto.bucketName(), requestDto.rootPath(), requestDto.decodedPath())) {
            throw new AlreadyExistException(requestDto.decodedPath());
        }
        List<ResourceResponseDto> result = new ArrayList<>();

        for (MultipartFile file : files) {
            long maxBytes = maxFileSize.toBytes();
            long fileSize = file.getSize();

            if (fileSize > maxBytes) {
                throw new FileTooLargeException("TooLarge.file");
            }
            String objectPath = requestDto.decodedPath();
            String objectName = file.getOriginalFilename();

            if (fileSize > 0 && objectName != null && !objectName.isBlank()) {
                try (InputStream is = file.getInputStream()) {
                    uploadObject(requestDto.bucketName(), requestDto.rootPath() + objectPath + objectName,
                            new UploadDto(is, fileSize, -1, file.getContentType()));
                    result.add(new ResourceResponseDto(objectPath, objectName, fileSize));
                } catch (IOException e) {
                    throw new RuntimeException("Ошибка при загрузке файла в MinIO: " + objectName, e);
                }
            } else {
                createFolder(requestDto.bucketName(), requestDto.rootPath() + objectPath + objectName);
            }
        }
        return result;
    }

    public InputStream download(RequestDto requestDto) {

        if (!objectAlreadyExist(requestDto.bucketName(), requestDto.rootPath(), requestDto.decodedPath())) {
            throw new ObjectNotExistException(requestDto.rootPath() + requestDto.decodedPath());
        }
        String fullPath = requestDto.rootPath() + requestDto.decodedPath();

        if (requestDto.decodedPath().endsWith("/")) {
            try {
                PipedInputStream pis = new PipedInputStream();
                PipedOutputStream pos = new PipedOutputStream(pis);

                new Thread(() -> {
                    try (ZipOutputStream zos = new ZipOutputStream(pos)) {
                        Iterable<Result<Item>> listObjects = getListObjects(requestDto.bucketName(), fullPath, true);

                        for (Result<Item> object : listObjects) {

                            if (!object.get().isDir()) {
                                String name = object.get().objectName();
                                zos.putNextEntry(new ZipEntry(name.substring(fullPath.length())));
                                try (InputStream is = getObjectStream(requestDto.bucketName(), name)) {
                                    is.transferTo(zos);
                                }
                                zos.closeEntry();
                            }
                        }
                        zos.finish();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).start();
                return pis;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                return getObjectStream(requestDto.bucketName(), fullPath);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public ResourceResponseDto rename(RenameRequestDto requestDto) {
        if (objectAlreadyExist(requestDto.bucketName(), requestDto.rootPath(), requestDto.decodedPathFrom())) {

            if (!objectAlreadyExist(requestDto.bucketName(), requestDto.rootPath(), requestDto.decodedPathTo())) {

                if (requestDto.decodedPathFrom().endsWith("/")) {
                    renameFolder(requestDto);
                } else {
                    renameObject(requestDto);
                }
                return new ResourceResponseDto(requestDto.decodedPathTo(),
                        pathCreator.getObjectName(requestDto.decodedPathTo()));
            }
            throw new AlreadyExistException("Resource '" + requestDto.decodedPathTo() + "' already exist");
        }
        throw new NotFoundException("Resource '" + requestDto.decodedPathFrom() + "' is not found");
    }

    private boolean objectAlreadyExist(String bucketName, String rootPath, String decodedPath) {
        try {
            getStatObject(bucketName, rootPath, decodedPath);
        } catch (ObjectNotExistException e) {
            return false;
        }
        return true;
    }

    private void renameObject(RenameRequestDto requestDto) {
        copyObject(requestDto);
        removeObject(requestDto);
    }

    private void renameFolder(RenameRequestDto requestDto) {
        Iterable<Result<Item>> objects = getListObjects(
                requestDto.bucketName(), requestDto.rootPath() + requestDto.decodedPathFrom(), false);

        for (Result<Item> result : objects) {
            Item item;
            try {
                item = result.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            String objectName = pathCreator.getObjectName(item.objectName());
            String newPathFrom = requestDto.decodedPathFrom() + objectName;
            String newPathTo = requestDto.decodedPathTo() + objectName;
            RenameRequestDto newRequestDto = new RenameRequestDto(requestDto.bucketName(), requestDto.rootPath(), newPathFrom, newPathTo);

            if (objectName.endsWith("/")) {
                renameFolder(newRequestDto);
            } else {
                renameObject(newRequestDto);
            }
        }
    }

    public List<ResourceResponseDto> search(RequestDto requestDto) {
        List<ResourceResponseDto> answer = new ArrayList<>();

        Iterable<Result<Item>> results = getListObjects(requestDto.bucketName(), requestDto.rootPath(), true);

        for (Result<Item> result : results) {
            String objectPathWithName;

            try {
                objectPathWithName = result.get().objectName();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            String objectPath = pathCreator.getObjectPath(objectPathWithName);
            String objectName = pathCreator.getObjectName(objectPathWithName);

            if (objectName.toLowerCase().contains(requestDto.decodedPath().toLowerCase())) {
                String objectPathWithoutRoot = objectPath.substring(requestDto.rootPath().length());
                ResourceResponseDto dto;

                if (objectPathWithName.endsWith("/") && !objectPathWithName.equals(requestDto.rootPath())) {
                    dto = new ResourceResponseDto(objectPathWithoutRoot, objectName);
                } else {
                    StatObjectResponse objectStat = getStatObject(requestDto.bucketName(),
                            requestDto.rootPath(), objectPathWithName);
                    dto = new ResourceResponseDto(objectPathWithoutRoot, objectName, objectStat.size());
                }
                answer.add(dto);
            }
        }
        return answer;
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
            throw new CreateNewFolderException(e.getMessage(), e);
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
            throw new RuntimeException(e);
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

    private StatObjectResponse getStatObject(String bucketName, String rootPath, String objectName) {
        try {
            return minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(rootPath + objectName)
                            .build());
        } catch (Exception e) {
            throw new ObjectNotExistException(objectName);
        }
    }

    private void uploadObject(String bucketName, String objectName, UploadDto uploadDto) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(uploadDto.inputStream(), uploadDto.fileSize(), uploadDto.i())
                            .contentType(uploadDto.contentType())
                            .build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void copyObject(RenameRequestDto requestDto) {
        try {
            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(requestDto.bucketName())
                            .object(requestDto.rootPath() + requestDto.decodedPathTo())
                            .source(CopySource.builder()
                                    .bucket(requestDto.bucketName())
                                    .object(requestDto.rootPath() + requestDto.decodedPathFrom())
                                    .build())
                            .build());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void removeObject(RenameRequestDto requestDto) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(requestDto.bucketName())
                            .object(requestDto.rootPath() + requestDto.decodedPathFrom())
                            .build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void removeObjects(String bucketName, Iterable<Result<Item>> deletedObjects) {
        List<DeleteObject> objects = new LinkedList<>();

        for (Result<Item> result : deletedObjects) {
            try {
                objects.add(new DeleteObject(result.get().objectName()));
            } catch (Exception e) {
                throw new RuntimeException(e);
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
                throw new RuntimeException(e);
            }
        }
    }
}
