package com.kronos.olympus;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// Profil "test" : Flyway off, schéma bâti par Hibernate (voir application-test.yml)
@ActiveProfiles("test")
@SpringBootTest(classes = OlympusApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OlympusApplicationTests {

	@Test
	void contextLoads() {
	}

}
