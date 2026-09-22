package ru.cloudStorage.CloudStorage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;


public class FileUploadDto {

        @Schema(description = "Список файлов для загрузки", type = "array", implementation = byte[].class)
        private MultipartFile[] object;

        public MultipartFile[] getObject() {
                return object;
        }

        public void setObject(MultipartFile[] object) {
                this.object = object;
        }
}
