package org.example.lab1;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class Lab1ApplicationTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void protectedEndpointRejectsAnonymousRequest() throws Exception {
        mockMvc.perform(get("/api/data"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginReturnsJwtAndAllowsReadingProtectedData() throws Exception {
        String token = login();

        mockMvc.perform(get("/api/data")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Security checklist"));
    }

    @Test
    void createdPostIsEscapedInApiResponse() throws Exception {
        String token = login();

        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"<script>alert(1)</script>","body":"Hello <b>world</b>"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("&lt;script&gt;alert(1)&lt;/script&gt;"));
    }

    private String login() throws Exception {
        return com.jayway.jsonpath.JsonPath.read(
                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"username":"admin","password":"local-demo-password"}
                                        """))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.token"
        );
    }
}
