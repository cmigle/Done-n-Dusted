package lt.ktu.donendusted.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import lt.ktu.donendusted.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** The nightly demo reset brings back exactly the seed data, whatever visitors did, and leaves the admin alone. */
class DemoResetIntegrationTest extends IntegrationTest {

    @Autowired
    private DemoResetService demoReset;

    @Autowired
    private DataSource dataSource;

    @Test
    void theTableListCoversEveryTable() {
        List<String> tables = jdbc().queryForList("select table_name from information_schema.tables"
                + " where table_schema = 'public' and table_name <> 'flyway_schema_history'", String.class);

        assertThat(tables).containsExactlyInAnyOrderElementsOf(DemoResetService.TABLES);
    }

    @Test
    void resetUndoesWhatVisitorsChanged() throws Exception {
        demoReset.reset();
        Map<String, List<Map<String, Object>>> seed = snapshot();
        String adminHash = adminHash();

        // A visitor deletes a demo guild, ends a friendship, signs up and creates a guild of their own;
        // meanwhile the admin's password differs from the seed one, as it does on the live demo.
        deleteAs(token("rokas"), "/api/guilds/2").andExpect(status().isNoContent());
        jdbc().update("delete from friendship");
        TestUser visitor = newUser();
        createGuild(visitor.token());
        jdbc().update("update app_user set password_hash = 'set-from-ADMIN_PASSWORD' where username = 'admin'");

        try {
            demoReset.reset();
            assertThat(adminHash()).isEqualTo("set-from-ADMIN_PASSWORD");
        } finally {
            jdbc().update("update app_user set password_hash = ? where username = 'admin'", adminHash);
        }

        assertThat(snapshot()).isEqualTo(seed);
        login(visitor.username(), PASSWORD).andExpect(status().isUnauthorized());
        assertThat(jdbc().queryForObject("select count(*) from information_schema.schemata where schema_name = ?",
                Long.class, DemoResetService.SEED_SCHEMA)).isZero();

        // New rows continue after the seed ids instead of colliding with them
        long maxSeedGuild = jdbc().queryForObject("select max(id) from guild", Long.class);
        assertThat(createGuild(token("rokas"))).isEqualTo(maxSeedGuild + 1);
        demoReset.reset();
    }

    /** Every row of every table, without the timestamps that the seed fills with now(). */
    private Map<String, List<Map<String, Object>>> snapshot() {
        Map<String, List<Map<String, Object>>> tables = new LinkedHashMap<>();
        for (String table : DemoResetService.TABLES) {
            String columns = String.join(", ", jdbc().queryForList("select column_name from information_schema.columns"
                    + " where table_schema = 'public' and table_name = ? and data_type not like 'timestamp%'"
                    + " order by ordinal_position", String.class, table));
            tables.put(table, jdbc().queryForList("select " + columns + " from public." + table + " order by id"));
        }
        return tables;
    }

    private String adminHash() {
        return jdbc().queryForObject("select password_hash from app_user where username = 'admin'", String.class);
    }

    private JdbcTemplate jdbc() {
        return new JdbcTemplate(dataSource);
    }
}
