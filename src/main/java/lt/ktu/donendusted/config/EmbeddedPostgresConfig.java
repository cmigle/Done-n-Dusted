package lt.ktu.donendusted.config;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import javax.sql.DataSource;
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

    @Bean(destroyMethod = "close")
    public EmbeddedPostgres embeddedPostgres() throws IOException {
        return EmbeddedPostgres.builder().setPort(55432).start();
    }

    @Bean
    public DataSource dataSource(EmbeddedPostgres postgres) {
        return postgres.getPostgresDatabase();
    }
}
