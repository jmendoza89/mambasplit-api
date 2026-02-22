package io.mambatech.mambasplit;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
abstract class ITBase {
  @Value("${spring.datasource.url}")
  private String datasourceUrl;

  @BeforeEach
  void assertSafeDatabaseTarget() {
    if (datasourceUrl != null && datasourceUrl.matches("jdbc:postgresql://.*/mambasplit(?:\\?.*)?$")) {
      throw new IllegalStateException(
        "Refusing to run integration tests against non-test database: " + datasourceUrl
      );
    }
  }
}
