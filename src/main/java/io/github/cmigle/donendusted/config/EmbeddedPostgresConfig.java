package io.github.cmigle.donendusted.config;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * "dev" profilis paleidzia tikra PostgreSQL 18 procesa is atsisiunciamu binariniu failu,
 * todel demonstracijai nereikia nei Docker, nei instaliuotos duomenu bazes.
 * "prod" profilyje naudojamas iprastas application-prod.yml apibreztas DataSource.
 */
@Configuration
@Profile("dev")
public class EmbeddedPostgresConfig {

    // destroyMethod = "close": sustabdzius programa, Spring uzdaro ir PostgreSQL procesa.
    // Numatytasis portas fiksuotas (55432), kad demonstracijos metu butu galima prisijungti per DBeaver/psql.
    // app.embedded-db.port=0 - atsitiktinis laisvas portas (testams ir antrai kopijai salia demo).
    // Duomenys laikini - kiekvienas paleidimas pradeda nuo svarios DB, Flyway vel ja uzpildo.
    @Bean(destroyMethod = "close")
    public EmbeddedPostgres embeddedPostgres(@Value("${app.embedded-db.port:55432}") int port) throws IOException {
        return EmbeddedPostgres.builder().setPort(port).start();
    }

    @Bean
    public DataSource dataSource(EmbeddedPostgres postgres) {
        return postgres.getPostgresDatabase();
    }
}
