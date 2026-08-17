package com.kronos.olympus.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.CRC32;

import static org.assertj.core.api.Assertions.assertThat;

class AppliedMigrationsAreImmutableTest {

    private static final Path MIGRATIONS = Path.of("src/main/resources/db/migration");

    private static final Map<String, Integer> DEPLOYED_CHECKSUMS = new LinkedHashMap<>() {{
        put("V9__ciqual_identity_and_nutrients.sql", -2124974891);
        put("V10__daily_log_palier_a_defaults.sql", -750258494);
    }};

    @Test
    void deployedMigrations_areByteIdenticalToWhatFlywayApplied() throws IOException {
        for (Map.Entry<String, Integer> entry : DEPLOYED_CHECKSUMS.entrySet()) {
            Path file = MIGRATIONS.resolve(entry.getKey());
            assertThat(file).as("migration %s", entry.getKey()).exists();
            assertThat(flywayChecksum(file))
                    .as("%s a ete modifiee apres avoir ete appliquee : Flyway refusera de demarrer",
                            entry.getKey())
                    .isEqualTo(entry.getValue());
        }
    }

    private int flywayChecksum(Path file) throws IOException {
        CRC32 crc32 = new CRC32();
        for (String line : Files.readAllLines(file)) {
            crc32.update(line.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        return (int) crc32.getValue();
    }
}
