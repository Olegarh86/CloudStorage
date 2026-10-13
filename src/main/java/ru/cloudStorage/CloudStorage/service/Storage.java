package ru.cloudStorage.CloudStorage.service;

import org.springframework.core.io.InputStreamResource;
import org.springframework.web.multipart.MultipartFile;
import ru.cloudStorage.CloudStorage.dto.RenameRequestDto;
import ru.cloudStorage.CloudStorage.dto.RequestDto;
import ru.cloudStorage.CloudStorage.dto.ResourceResponseDto;

import java.util.List;

public interface Storage {
    void createNewRootFolder(Long id);

    List<ResourceResponseDto> getAllObjectsFromDirectory(RequestDto requestDto);

    ResourceResponseDto createNewFolder(RequestDto requestDto);

    ResourceResponseDto getObjectByPathWithName(RequestDto requestDto);

    void delete(RequestDto requestDto);

    List<ResourceResponseDto> upload(RequestDto requestDto, List<MultipartFile> files);

    InputStreamResource download(RequestDto requestDto);

    ResourceResponseDto rename(RenameRequestDto renameRequestDto);

    List<ResourceResponseDto> search(RequestDto requestDto);
}
