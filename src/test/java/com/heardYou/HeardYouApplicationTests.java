package com.heardYou;

import com.heardYou.condition.ConditionRepository;
import com.heardYou.condition.ConditionSymptomRepository;
import com.heardYou.source.MedicalSourceRepository;
import com.heardYou.symptom.SymptomRepository;
import com.heardYou.warning.WarningSignRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Spring context smoke test. Loads the full application context but mocks out
 * all JPA repositories so no live database is required.
 *
 * The test verifies that the application wires correctly, not that data is
 * persisted. No DB_PASSWORD environment variable is needed.
 */
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration," +
                "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration," +
                "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration"
})
class HeardYouApplicationTests {

    @MockitoBean
    ConditionRepository conditionRepository;
    @MockitoBean
    ConditionSymptomRepository conditionSymptomRepository;
    @MockitoBean
    SymptomRepository symptomRepository;
    @MockitoBean
    WarningSignRepository warningSignRepository;
    @MockitoBean
    MedicalSourceRepository medicalSourceRepository;

    @Test
    void contextLoads() {
    }
}
