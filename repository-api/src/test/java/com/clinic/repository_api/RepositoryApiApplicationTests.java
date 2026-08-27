package com.clinic.repository_api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * The dev profile now points at a FILE-backed H2 (repository-api/data/) so that local
 * work survives a restart. Tests must not inherit that: a test run would otherwise
 * write demo data into the developer's working database, and a failed run could leave
 * it in a state that breaks the next `mvn test`.
 *
 * Overridden here rather than in src/test/resources/application.properties, because a
 * file of that name on the test classpath shadows the main one entirely instead of
 * merging with it — every unrelated setting would have to be duplicated to keep it.
 */
@SpringBootTest
@TestPropertySource(properties = {
		"spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
class RepositoryApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
