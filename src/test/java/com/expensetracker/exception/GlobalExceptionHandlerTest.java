package com.expensetracker.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.NoSuchElementException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GlobalExceptionHandlerTest.TestController.class)
@Import(GlobalExceptionHandler.class)
public class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @TestConfiguration
    static class Config {
        @Bean
        public TestController testController() {
            return new TestController();
        }
    }

    @RestController
    public static class TestController {
        @GetMapping("/test/not-found")
        public void throwNotFound() {
            throw new NoSuchElementException("Resource not found");
        }

        @GetMapping("/test/bad-request")
        public void throwBadRequest() {
            throw new IllegalArgumentException("Invalid argument provided");
        }

        @GetMapping("/test/generic-error")
        public void throwGeneric() throws Exception {
            throw new Exception("Server error occurred");
        }

        @PostMapping("/test/validation")
        public void throwValidation(@Valid @RequestBody TestDto dto) {
        }
    }

    public static class TestDto {
        @NotBlank(message = "Name must not be blank")
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Test
    @DisplayName("Should handle NoSuchElementException and return 404")
    void testHandleNotFound() throws Exception {
        mockMvc.perform(get("/test/not-found")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Resource not found"))
                .andExpect(jsonPath("$.path").value("/test/not-found"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Should handle IllegalArgumentException and return 400")
    void testHandleBadRequest() throws Exception {
        mockMvc.perform(get("/test/bad-request")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid argument provided"))
                .andExpect(jsonPath("$.path").value("/test/bad-request"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Should handle Exception and return 500")
    void testHandleGenericException() throws Exception {
        mockMvc.perform(get("/test/generic-error")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred: Server error occurred"))
                .andExpect(jsonPath("$.path").value("/test/generic-error"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Should handle MethodArgumentNotValidException and return 400 with details")
    void testHandleValidationException() throws Exception {
        mockMvc.perform(post("/test/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("name: Name must not be blank")))
                .andExpect(jsonPath("$.path").value("/test/validation"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
