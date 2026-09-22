package ru.cloudStorage.CloudStorage;


import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import ru.cloudStorage.CloudStorage.dto.AuthRequest;

import static org.springframework.http.RequestEntity.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class UserAuthIntegrationTest extends BaseIntegrationTest {

    @Test
    void successSignUpTest() throws Exception {
        mockMvc.perform(post("/api/auth/sign-up", new AuthRequest("Tod", "Tod123")
                .contentType(MediaType.APPLICATION_JSON)));
    }

}