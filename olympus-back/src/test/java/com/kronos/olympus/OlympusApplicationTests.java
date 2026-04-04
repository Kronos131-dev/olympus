package com.kronos.olympus;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// On désactive le test de chargement complet du contexte si la base de données n'est pas lancée
@SpringBootTest(classes = OlympusApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OlympusApplicationTests {

	@Test
	void contextLoads() {
	}

}
