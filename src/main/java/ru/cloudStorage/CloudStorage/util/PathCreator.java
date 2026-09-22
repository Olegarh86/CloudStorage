package ru.cloudStorage.CloudStorage.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.cloudStorage.CloudStorage.config.MinioProperties;
import ru.cloudStorage.CloudStorage.dto.FolderDto;
import ru.cloudStorage.CloudStorage.dto.RenameRequestDto;
import ru.cloudStorage.CloudStorage.dto.RequestDto;
import ru.cloudStorage.CloudStorage.exception.NotFoundException;
import ru.cloudStorage.CloudStorage.exception.ValidateException;
import ru.cloudStorage.CloudStorage.repository.UserRepository;

import java.net.URLDecoder;

import static java.nio.charset.StandardCharsets.UTF_8;

@Component
public class PathCreator {

    private final MinioProperties minioProperties;
    private final UserRepository userRepository;

    @Autowired
    public PathCreator(MinioProperties minioProperties, UserRepository userRepository) {
        this.minioProperties = minioProperties;
        this.userRepository = userRepository;
    }

    public RequestDto createRequestDto(String username, String queryPath) {
        String bucketName = minioProperties.getBucketName();
        String rootPath = getRootPath(username);
        String decodedPath;

        if (queryPath.isBlank()) {
            decodedPath = "";
        } else {
            decodedPath = decodePath(queryPath);

            if (decodedPath.endsWith("/")) {
                validateFolderName(getObjectName(decodedPath));
            } else {
                validatePath(getObjectPath(decodedPath));
            }
        }
        return new RequestDto(bucketName, rootPath, decodedPath);
    }

    public RenameRequestDto createRequestRenameDto(String username, String from, String to) {
        String bucketName = minioProperties.getBucketName();
        String rootPath = getRootPath(username);
        String decodedPathFrom = decodePath(from);
        validatePath(getObjectPath(decodedPathFrom));
        String decodedPathTo = decodePath(to);
        validatePath(getObjectPath(decodedPathTo));
        return new RenameRequestDto(bucketName, rootPath, decodedPathFrom, decodedPathTo);
    }

    public FolderDto createFolderDto(RequestDto dto) {
        String bucketName = dto.bucketName();
        String rootPath = dto.rootPath();
        String pathWithoutName = getObjectPath(dto.decodedPath());
        String name = getObjectName(dto.decodedPath());
        String fullPath = rootPath + pathWithoutName + name;
        return new FolderDto(fullPath, bucketName, rootPath, pathWithoutName, name);
    }

    public String getObjectPath(String path) {
        if (path.isBlank()) {
            return "";
        }

        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);

            if (path.isBlank()) {
                return "";
            }
            int lastSlash = path.lastIndexOf("/");

            if (lastSlash == -1) {
                return "";
            } else {
                return path.substring(0, lastSlash + 1);
            }
        }
        int lastSlash = path.lastIndexOf("/");

        if (lastSlash == -1) {
            return "";
        } else {
            return path.substring(0, lastSlash + 1);
        }
    }

    public String getObjectName(String path) {
        int lastSlash;

        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
            lastSlash = path.lastIndexOf("/");

            if (lastSlash == -1) {
                return path + "/";
            } else {
                return path.substring(lastSlash + 1) + "/";
            }
        }
        lastSlash = path.lastIndexOf("/");

        if (lastSlash == -1) {
            return path;
        } else {
            return path.substring(lastSlash + 1);
        }
    }

    private String getRootPath(String username) {
        Long id = userRepository.findByUserName(username)
                .orElseThrow(() -> new NotFoundException("User with name " + username + "not found"))
                .getId();
        return "user-" + id + "-files/";
    }

    private void validatePath(String path) {
        if (path.isBlank()) {
            return;
        }
        if (!path.matches("^[^\\\\:*?\"<>|]+$")) {
            throw new ValidateException("Invalid path: " + path);
        }
    }

    private String decodePath(String path) {
        return URLDecoder.decode(path, UTF_8);
    }

    private static void validateFolderName(String folderName) {
        String folderNameWithoutSlash = folderName.substring(0, folderName.length() - 1);

        if (folderNameWithoutSlash.length() > 200 || !folderNameWithoutSlash.matches("^[^/\\\\:*?\"<>|]+$")) {
            throw new ValidateException(folderName);
        }
    }
}
