package ru.cloudStorage.CloudStorage;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import ru.cloudStorage.CloudStorage.dto.AuthRequest;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
public class ResourceControllerTest extends BaseIntegrationTest {
    private Cookie[] myCookie;

    @BeforeEach
    public void createCookie() throws Exception {
        AuthRequest authRequest = new AuthRequest(name, password);
        String requestBody = mapper.writeValueAsString(authRequest);

        var result = mockMvc.perform(post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(name))
                .andReturn();

        assertTrue(userRepository.findByUserName(name).isPresent());
        myCookie = result.getResponse().getCookies();
    }

    @Test
    void successGetResourceTest() throws Exception {
        mockMvc.perform(get("/api/resource")
                        .cookie(myCookie)
                        .param("path", ""))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value(""))
                .andExpect(jsonPath("$.type").value("DIRECTORY"));
    }

    @Test
    void successUploadResourceTest() throws Exception {
        MockMultipartFile mockFile = new MockMultipartFile(
                "object",
                "example.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "This text is written as an example".getBytes()
        );

        mockMvc.perform(multipart("/api/resource")
                        .cookie(myCookie)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .file(mockFile)
                        .param("path", ""))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("[0].path").value(""))
                .andExpect(jsonPath("[0].name").value("example.txt"))
                .andExpect(jsonPath("[0].size").value(34))
                .andExpect(jsonPath("[0].type").value("FILE"));
    }

    @Test
    void AlreadyExistFileUploadTest() throws Exception {
        MockMultipartFile mockFile = new MockMultipartFile(
                "object",
                "example.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "This text is written as an example".getBytes()
        );
        mockMvc.perform(multipart("/api/resource")
                .cookie(myCookie)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .file(mockFile)
                .param("path", ""));

        mockMvc.perform(multipart("/api/resource")
                        .cookie(myCookie)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .file(mockFile)
                        .param("path", ""))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("[0].path").value(""))
                .andExpect(jsonPath("[0].name").value("example1.txt"))
                .andExpect(jsonPath("[0].size").value(34))
                .andExpect(jsonPath("[0].type").value("FILE"));
    }

    @Test
    void SuccessDeleteTest() throws Exception {
        MockMultipartFile mockFile = new MockMultipartFile(
                "object",
                "example.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "This text is written as an example".getBytes()
        );
        mockMvc.perform(multipart("/api/resource")
                .cookie(myCookie)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .file(mockFile)
                .param("path", ""));

        mockMvc.perform(delete("/api/resource")
                        .cookie(myCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("path", "example.txt"))
                .andExpect(status().isNoContent());
    }

    @Test
    void DeleteNotExistObjectTest() throws Exception {
        MockMultipartFile mockFile = new MockMultipartFile(
                "object",
                "example.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "This text is written as an example".getBytes()
        );
        mockMvc.perform(multipart("/api/resource")
                .cookie(myCookie)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .file(mockFile)
                .param("path", ""));

        mockMvc.perform(delete("/api/resource")
                        .cookie(myCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("path", "example.txt"))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/resource")
                        .cookie(myCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("path", "example.txt"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Object not found: 'example.txt'"));
    }

    @Test
    void SuccessDownloadTest() throws Exception {
        MockMultipartFile mockFile = new MockMultipartFile(
                "object",
                "example.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "This text is written as an example".getBytes()
        );
        mockMvc.perform(multipart("/api/resource")
                .cookie(myCookie)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .file(mockFile)
                .param("path", ""));

        mockMvc.perform(get("/api/resource/download")
                        .cookie(myCookie)
                        .param("path", "example.txt"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM));
    }

    @Test
    void SearchResourceTest() throws Exception {
        MockMultipartFile mockFile = new MockMultipartFile(
                "object",
                "example.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "This text is written as an example".getBytes()
        );
        mockMvc.perform(multipart("/api/resource")
                .cookie(myCookie)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .file(mockFile)
                .param("path", ""));

        mockMvc.perform(get("/api/resource/search")
                        .cookie(myCookie)
                        .param("query", "t"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].path").value(""))
                .andExpect(jsonPath("$[0].name").value("example.txt"))
                .andExpect(jsonPath("$[0].size").value(34))
                .andExpect(jsonPath("$[0].type").value("FILE"));
    }

    @Test
    void RenameResourceTest() throws Exception {
        MockMultipartFile mockFile = new MockMultipartFile(
                "object",
                "example.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "This text is written as an example".getBytes()
        );
        mockMvc.perform(multipart("/api/resource")
                .cookie(myCookie)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .file(mockFile)
                .param("path", ""))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("[0].path").value(""))
                .andExpect(jsonPath("[0].name").value("example.txt"))
                .andExpect(jsonPath("[0].size").value(34))
                .andExpect(jsonPath("[0].type").value("FILE"));

        mockMvc.perform(post("/api/resource/move")
                        .cookie(myCookie)
                        .param("from", "example.txt")
                        .param("to", "exampleExample.txt"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("exampleExample.txt"))
                .andExpect(jsonPath("$.size").value(34))
                .andExpect(jsonPath("$.type").value("FILE"));

        mockMvc.perform(get("/api/resource")
                        .cookie(myCookie)
                        .param("path", "example.txt"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Object not found: 'Object not exist: example.txt'"));

        mockMvc.perform(post("/api/directory")
                        .cookie(myCookie)
                        .param("path", "TestFolder/"))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("TestFolder/"))
                .andExpect(jsonPath("$.type").value("DIRECTORY"));

        mockMvc.perform(post("/api/resource/move")
                        .cookie(myCookie)
                        .param("from", "TestFolder/")
                        .param("to", "TestFolderRenamed/"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("TestFolderRenamed/"))
                .andExpect(jsonPath("$.type").value("DIRECTORY"));

        mockMvc.perform(get("/api/directory")
                        .cookie(myCookie)
                        .param("path", "TestFolder/"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Object not found: 'Root folder not exist: TestFolder/'"));
    }

    @Test
    void RenameNotFoundExceptionTest() throws Exception {

        mockMvc.perform(post("/api/resource/move")
                        .cookie(myCookie)
                        .param("from", "example.txt")
                        .param("to", "exampleExample.txt"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Resource not found: 'example.txt'"));
    }

    @Test
    void RenameResourceAlreadyExistTest() throws Exception {
        MockMultipartFile mockFile = new MockMultipartFile(
                "object",
                "example.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "This text is written as an example".getBytes()
        );
        mockMvc.perform(multipart("/api/resource")
                .cookie(myCookie)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .file(mockFile)
                .param("path", ""));

        mockMvc.perform(post("/api/resource/move")
                        .cookie(myCookie)
                        .param("from", "example.txt")
                        .param("to", "example.txt"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Resource already exist: 'example.txt'"));
    }
}
