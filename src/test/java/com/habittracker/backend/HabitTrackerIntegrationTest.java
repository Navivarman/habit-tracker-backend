package com.habittracker.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional // Automatically rolls back database operations after each test finishes
public class HabitTrackerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private Map<String, String> userCredentials;

    @BeforeEach
    void setUp() {
        userCredentials = new HashMap<>();
        userCredentials.put("username", "test_automation_user");
        userCredentials.put("email", "automation@example.com");
        userCredentials.put("password", "TestPass123");
    }

    @Test
    void testCompleteUserAndHabitPipeline() throws Exception {

        // 1️⃣ TEST AUTOMATED USER REGISTRATION
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userCredentials)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("test_automation_user"))
                .andExpect(jsonPath("$.id").exists());

        // 2️⃣ TEST DUPLICATE REGISTRATION PREVENTION
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userCredentials)))
                .andExpect(status().isBadRequest());

        // 3️⃣ TEST USER LOGIN SUCCESS
        Map<String, String> loginPayload = new HashMap<>();
        loginPayload.put("username", "test_automation_user");
        loginPayload.put("password", "TestPass123");

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("test_automation_user"))
                .andReturn().getResponse().getContentAsString();

        // Extract generated ID dynamically from the running payload
        Integer userId = com.jayway.jsonpath.JsonPath.read(loginResponse, "$.id");

        // 4️⃣ TEST HABIT CREATION FOR THE NEW USER
        Map<String, Object> habitPayload = new HashMap<>();
        habitPayload.put("userId", userId);
        habitPayload.put("title", "Automated Testing Exercise");
        habitPayload.put("reminderTime", "06:00:00");

        mockMvc.perform(post("/api/habits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(habitPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Automated Testing Exercise"));
    }
}