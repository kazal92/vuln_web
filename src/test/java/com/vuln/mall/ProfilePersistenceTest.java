package com.vuln.mall;

import com.vuln.mall.entity.User;
import com.vuln.mall.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ProfilePersistenceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    public void testProfileImagePersistence() throws Exception {
        // 1. Create a test user in DB if not exists (or use existing one)
        // DataInitializer runs on startup, so 'guest' should exist.
        // We verify finding 'guest'.
        User guest = userRepository.findByUsername("guest");
        assertNotNull(guest, "Guest user should exist from DataInitializer");

        // 2. Simulate Login via Session
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", "guest");
        session.setAttribute("role", "USER");

        // 3. Upload a file
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-avatar.png",
                "image/png",
                "dummy image content".getBytes());

        mockMvc.perform(multipart("/profile/upload")
                .file(file)
                .session(session))
                .andExpect(status().isOk());

        // 4. Verify DB persistence using Repository directly (to check immediate flush)
        User updatedGuest = userRepository.findByUsername("guest");
        assertEquals("/uploads/test-avatar.png", updatedGuest.getProfileImageUrl(),
                "Profile image URL should be persisted in DB immediately after upload");

        // 5. Verify retrieval via Controller (simulating page reload)
        mockMvc.perform(get("/profile")
                .session(session))
                .andExpect(status().isOk())
                .andReturn(); // In a real test we'd check model attribute, but DB check above is stronger
                              // proof of persistence.

        System.out.println("TEST SUCCESS: Image URL persisted: " + updatedGuest.getProfileImageUrl());
    }
}
