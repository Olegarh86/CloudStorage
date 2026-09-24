package ru.cloudStorage.CloudStorage;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import ru.cloudStorage.CloudStorage.dto.AuthRequest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class UserControllerTest extends BaseIntegrationTest{
    @Test
    void successSignUpTest() throws Exception {
        AuthRequest authRequest = new AuthRequest(name, password);
        String requestBody = mapper.writeValueAsString(authRequest);

        var result = mockMvc.perform(post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn();

        Cookie cookie = result.getResponse().getCookie("SESSION");

        Assertions.assertNotNull(cookie);
        mockMvc.perform(get("/api/user/me")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(name));
    }

    @Test
    void unsuccessfulSignUpTest() throws Exception {
                mockMvc.perform(get("/api/user/me"))
                .andExpect(status().isUnauthorized());
    }
}
