package com.heardYou.common;

import com.heardYou.condition.ConditionRepository;
import com.heardYou.condition.ConditionSymptomRepository;
import com.heardYou.config.DataLoader;
import com.heardYou.source.MedicalSourceRepository;
import com.heardYou.symptom.SymptomRepository;
import com.heardYou.warning.WarningSignRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.sql.DataSource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test: exercises GET /api/health through the real DispatcherServlet
 * and Spring MVC handler mapping chain.
 *
 * Unlike HealthControllerTest (which calls the Java method directly), this test
 * proves that the route is actually registered and reachable via HTTP.
 *
 * The full application context is loaded but JPA, DataSource, and DataLoader are
 * mocked so no database is needed.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.autoconfigure.exclude=" +
                        "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration," +
                        "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration," +
                        "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration"
        }
)
class HealthEndpointMvcTest {

    @Autowired
    private WebApplicationContext wac;

    // ── mock beans — same set as HeardYouApplicationTests ────────────────────

    @MockitoBean DataSource dataSource;
    @MockitoBean DataLoader dataLoader;
    @MockitoBean ConditionRepository conditionRepository;
    @MockitoBean ConditionSymptomRepository conditionSymptomRepository;
    @MockitoBean SymptomRepository symptomRepository;
    @MockitoBean WarningSignRepository warningSignRepository;
    @MockitoBean MedicalSourceRepository medicalSourceRepository;

    // ── MockMvc ───────────────────────────────────────────────────────────────

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    // ── tests ─────────────────────────────────────────────────────────────────

    @Test
    void getApiHealth_returns200WithCorrectJsonFields() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("HeardYou API"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    /**
     * Render's built-in health checker (and any reverse-proxy that strips /api)
     * may probe GET /health instead of GET /api/health.
     * Both paths must return HTTP 200.
     */
    @Test
    void getHealth_barePathAlso_returns200() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void getUnknownPath_returns404NotFiveHundred() throws Exception {
        mockMvc.perform(get("/api/this-route-does-not-exist"))
                .andExpect(status().isNotFound());
    }
}
