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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@Transactional
public class DirectoryControllerTest extends BaseIntegrationTest{
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
    void successCreateNewFolderTest() throws Exception {
        mockMvc.perform(post("/api/directory")
                        .cookie(myCookie)
                        .param("path", "TestFolder/"))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("TestFolder/"))
                .andExpect(jsonPath("$.type").value("DIRECTORY"));
    }

    @Test
    void createNewFolderParentFolderNotExistTest() throws Exception {
        mockMvc.perform(post("/api/directory")
                        .cookie(myCookie)
                        .param("path", "NotExistFolder/TestFolder/"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Object not found: 'Root folder not exist: NotExistFolder/'"));
    }

    @Test
    void createNewAlreadyExistsTest() throws Exception {
        mockMvc.perform(post("/api/directory")
                        .cookie(myCookie)
                        .param("path", "TestFolder/"))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("TestFolder/"))
                .andExpect(jsonPath("$.type").value("DIRECTORY"));

        mockMvc.perform(post("/api/directory")
                        .cookie(myCookie)
                        .param("path", "TestFolder/"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Object with name 'TestFolder/' already exists"));
    }

    @Test
    void getFolderTest() throws Exception {
        mockMvc.perform(post("/api/directory")
                        .cookie(myCookie)
                        .param("path", "TestFolder/"))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("TestFolder/"))
                .andExpect(jsonPath("$.type").value("DIRECTORY"));

        mockMvc.perform(post("/api/directory")
                        .cookie(myCookie)
                        .param("path", "TestFolder/TestFolder2/"))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.path").value("TestFolder/"))
                .andExpect(jsonPath("$.name").value("TestFolder2/"))
                .andExpect(jsonPath("$.type").value("DIRECTORY"));

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
                .param("path", "TestFolder/"));

        mockMvc.perform(get("/api/directory")
                        .cookie(myCookie)
                        .param("path", "TestFolder/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("[0].path").value("TestFolder/"))
                .andExpect(jsonPath("[0].name").value("example.txt"))
                .andExpect(jsonPath("[0].size").value(34))
                .andExpect(jsonPath("[0].type").value("FILE"))
                .andExpect(jsonPath("[1].path").value("TestFolder/"))
                .andExpect(jsonPath("[1].name").value("TestFolder2/"))
                .andExpect(jsonPath("[1].type").value("DIRECTORY"));
    }

    @Test
    void getNotExistFolderTest() throws Exception {
        mockMvc.perform(get("/api/directory")
                        .cookie(myCookie)
                        .param("path", "TestFolder/"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message")
                        .value("Object not found: 'Root folder not exist: TestFolder/'"));
    }
}
