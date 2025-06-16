package kali.requestbody;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class RequestBodyDtoTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @DisplayName("JSON 요청 본문을 @RequestBody로 매핑하면 DTO로 역직렬화된다")
    @Test
    void requestBody() throws Exception {
        // given
        RequestBodyDto dto = new RequestBodyDto("req", 1L, "pass", "email");
        String json = objectMapper.writeValueAsString(dto);

        // when & then
        mockMvc.perform(post("/request-body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("req"))
                .andExpect(jsonPath("$.age").value(1))
                .andExpect(jsonPath("$.password").value("pass"))
                .andExpect(jsonPath("$.email").value("email"));
    }
}
