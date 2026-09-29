package com.example.auroramarketplace.exception;

import io.grpc.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @ParameterizedTest(name = "{0} -> HTTP {1}")
    @CsvSource({
            "NOT_FOUND, 404",
            "INVALID_ARGUMENT, 400",
            "FAILED_PRECONDITION, 409",
            "UNAVAILABLE, 503",
            "DEADLINE_EXCEEDED, 504",
            "INTERNAL, 500",
            "UNKNOWN, 500",
    })
    void mapsGrpcStatusToHttpStatus(String grpcCode, int httpStatus) throws Exception {
        mockMvc.perform(get("/throw/{code}", grpcCode))
                .andExpect(status().is(httpStatus))
                .andExpect(jsonPath("$.error").value(grpcCode));
    }

    @Test
    void clientErrorIncludesDescription() throws Exception {
        mockMvc.perform(get("/throw/NOT_FOUND"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("description for NOT_FOUND"));
    }

    @Test
    void serverErrorHidesDescription() throws Exception {
        mockMvc.perform(get("/throw/INTERNAL"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", not(containsString("description"))));
    }

    @RestController
    static class ThrowingController {
        @GetMapping("/throw/{code}")
        String throwStatus(@PathVariable String code) {
            throw Status.fromCode(Status.Code.valueOf(code))
                    .withDescription("description for " + code)
                    .asRuntimeException();
        }
    }
}
