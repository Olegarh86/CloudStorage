package ru.cloudStorage.CloudStorage;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import ru.cloudStorage.CloudStorage.dto.AuthRequest;
import ru.cloudStorage.CloudStorage.repository.UserRepository;
import ru.cloudStorage.CloudStorage.service.AuthService;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.when;

@Transactional
public class UserAuthIntegrationTest extends BaseIntegrationTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final String name = "TestUser";
    private final String password = "TestUser123";
    @MockitoBean
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void successSignUpTest() throws Exception {
        AuthRequest authRequest = new AuthRequest(name, password);
        String requestBody = mapper.writeValueAsString(authRequest);

        mockMvc.perform(post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(name));

        assertTrue(userRepository.findByUserName(name).isPresent());
    }

    @Test
    void tooShortNameSignUpTest() throws Exception {
        AuthRequest authRequest = new AuthRequest("Kim", password);
        String requestBody = mapper.writeValueAsString(authRequest);

        mockMvc.perform(post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Username must be between 5 and 20 characters"));
    }

    @Test
    void tooLongNameSignUpTest() throws Exception {
        AuthRequest authRequest = new AuthRequest("Kimqwertyuioplkjhgfds", password);
        String requestBody = mapper.writeValueAsString(authRequest);

        mockMvc.perform(post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Username must be between 5 and 20 characters"));
    }

    @Test
    void invalidNameSignUpTest() throws Exception {
        AuthRequest authRequest = new AuthRequest("Kima$", password);
        String requestBody = mapper.writeValueAsString(authRequest);

        mockMvc.perform(post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid username format"));
    }

    @Test
    void userNameAlreadyExistTest() throws Exception {
        successSignUpTest();

        AuthRequest authRequest = new AuthRequest(name, password);
        String requestBody = mapper.writeValueAsString(authRequest);

        mockMvc.perform(post("/api/auth/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("User with name: " + name + " already exist, change another name"));
    }

    @Test
    void successSignInTest() throws Exception {
        successSignUpTest();

        AuthRequest authRequest = new AuthRequest(name, password);
        String requestBody = mapper.writeValueAsString(authRequest);

        mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("SESSION"));
    }

    @Test
    void invalidUserNameSingInTest() throws Exception {
        successSignUpTest();

        AuthRequest authRequest = new AuthRequest("TestUser%", password);
        String requestBody = mapper.writeValueAsString(authRequest);

        mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(cookie().doesNotExist("SESSION"))
                .andExpect(jsonPath("$.message").value("Invalid username format"));
    }

    @Test
    void invalidPasswordSingInTest() throws Exception {
        successSignUpTest();

        AuthRequest authRequest = new AuthRequest(name, "TestUser12");
        String requestBody = mapper.writeValueAsString(authRequest);

        mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized())
                .andExpect(cookie().doesNotExist("SESSION"))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void internalServerErrorSingInTest() throws Exception {
        successSignUpTest();

        AuthRequest authRequest = new AuthRequest(name, password);

        doThrow(new NullPointerException()).when(authService)
                .login(any(AuthRequest.class),any(MockHttpServletRequest.class));

        String requestBody = mapper.writeValueAsString(authRequest);

        mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().is5xxServerError())
                .andExpect(cookie().doesNotExist("SESSION"))
                /*.andExpect(jsonPath("$.message").value("Invalid username or password"))*/;
    }
}