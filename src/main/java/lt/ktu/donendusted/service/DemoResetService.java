package lt.ktu.donendusted.service;

import java.util.List;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Puts the public demo back into its seed state, so visitors can try everything with the demo logins
 * and nothing they change (or break) outlives the night.
 *
 * The seed is not duplicated here: the Flyway migrations are run into a scratch schema, and its rows are
 * copied over the live tables in one transaction. The live tables only see deletes and inserts, never DDL.
 * The {@code admin} account is kept as it is, because its password is set from ADMIN_PASSWORD at startup.
 */
@Service
public class DemoResetService {

    private static final Logger log = LoggerFactory.getLogger(DemoResetService.class);

    static final String SEED_SCHEMA = "demo_seed";
    static final String ADMIN = "admin";

    /** Every table the migrations create, children before parents. A test checks that none is missing. */
    static final List<String> TABLES =
            List.of("friendship", "refresh_token", "log_entry", "guild_membership", "quest", "guild", "app_user");

    private final DataSource dataSource;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final String[] locations;

    public DemoResetService(DataSource dataSource, PlatformTransactionManager transactionManager,
                            @Value("${spring.flyway.locations:classpath:db/migration}") String[] locations) {
        this.dataSource = dataSource;
        this.jdbc = new JdbcTemplate(dataSource);
        this.transaction = new TransactionTemplate(transactionManager);
        this.locations = locations;
    }

    /** Off unless app.demo-reset.cron is set (it is in the prod profile). */
    @Scheduled(cron = "${app.demo-reset.cron:-}", zone = "${app.demo-reset.zone:Europe/Vilnius}")
    public void scheduledReset() {
        try {
            reset();
        } catch (RuntimeException e) {
            log.error("Demo data reset failed; the data was left as it was", e);
        }
    }

    public void reset() {
        dropSeedSchema();
        try {
            Flyway.configure()
                    .dataSource(dataSource)
                    .schemas(SEED_SCHEMA)
                    .defaultSchema(SEED_SCHEMA)
                    .createSchemas(true)
                    .locations(locations)
                    .load()
                    .migrate();
            transaction.executeWithoutResult(status -> copySeed());
        } finally {
            dropSeedSchema();
        }
        log.info("Demo data reset to the seed state");
    }

    private void copySeed() {
        for (String table : TABLES) {
            jdbc.update("delete from public." + table + keepAdmin(table), adminArgs(table));
        }
        for (String table : TABLES.reversed()) {
            String columns = String.join(", ", jdbc.queryForList(
                    "select column_name from information_schema.columns where table_schema = ? and table_name = ?"
                            + " order by ordinal_position", String.class, SEED_SCHEMA, table));
            jdbc.update("insert into public." + table + " (" + columns + ") select " + columns
                    + " from " + SEED_SCHEMA + "." + table + keepAdmin(table), adminArgs(table));
            // explicit ids were copied, so move each sequence past them
            jdbc.queryForObject("select setval(pg_get_serial_sequence('public." + table + "', 'id'),"
                    + " greatest((select max(id) from public." + table + "), 1))", Long.class);
        }
    }

    private static String keepAdmin(String table) {
        return table.equals("app_user") ? " where username <> ?" : "";
    }

    private static Object[] adminArgs(String table) {
        return table.equals("app_user") ? new Object[] {ADMIN} : new Object[0];
    }

    private void dropSeedSchema() {
        jdbc.execute("drop schema if exists " + SEED_SCHEMA + " cascade");
    }
}
