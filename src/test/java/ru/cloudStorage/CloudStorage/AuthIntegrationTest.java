package ru.cloudStorage.CloudStorage;


import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import ru.cloudStorage.CloudStorage.dto.AuthRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
public class AuthIntegrationTest extends BaseIntegrationTest {

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
                .andExpect(jsonPath("$.message")
                        .value("Username must be between 5 and 20 characters"));
    }

    @Test
    void tooLongNameSignUpTest() throws Exception {
        AuthRequest authRequest = new AuthRequest("Kimqwertyuioplkjhgfds", password);
        String requestBody = mapper.writeValueAsString(authRequest);

        mockMvc.perform(post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Username must be between 5 and 20 characters"));
    }

    @Test
    void invalidNameSignUpTest() throws Exception {
        AuthRequest authRequest = new AuthRequest("Kima$", password);
        String requestBody = mapper.writeValueAsString(authRequest);

        mockMvc.perform(post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Invalid username format"));
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
                .andExpect(jsonPath("$.message")
                        .value("Invalid username format"));
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
                .andExpect(jsonPath("$.message")
                        .value("Invalid username or password"));
    }

    @Test
    void successSignOutTest() throws Exception {
        successSignUpTest();

        AuthRequest authRequest = new AuthRequest(name, password);
        String requestBody = mapper.writeValueAsString(authRequest);

        MvcResult mvcResult = mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("SESSION"))
                .andReturn();

        Cookie session = mvcResult.getResponse().getCookie("SESSION");
        Assertions.assertNotNull(session);

        MvcResult resultActions = mockMvc.perform(post("/api/auth/sign-out")
                        .cookie(session))
                .andExpect(status().isNoContent())
                .andExpect(cookie().exists("SESSION"))
                .andReturn();

        Cookie emptyCookie = resultActions.getResponse().getCookie("SESSION");
        Assertions.assertNotNull(emptyCookie);
        assertEquals(0, emptyCookie.getMaxAge());
    }
}