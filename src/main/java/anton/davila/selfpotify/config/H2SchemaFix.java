package anton.davila.selfpotify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Drops any stale discriminator CHECK constraints on the 'users' table that
 * exclude the 'USER' type literal.  Such constraints can appear in persistent
 * H2 file databases when Hibernate generated the constraint at a time when
 * only the 'ADMIN' subclass was processed, leaving 'USER' out of the IN-list.
 *
 * Runs after Hibernate's ddl-auto so every startup is self-healing: the
 * SchemaUpdate phase will not recreate a dropped CHECK constraint, so after
 * the first successful run the table stays clean.
 */
@Component
@ConditionalOnProperty(
        name = "spring.datasource.driverClassName",
        havingValue = "org.h2.Driver",
        matchIfMissing = true)
class H2SchemaFix implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(H2SchemaFix.class);

    private final JdbcTemplate jdbc;

    H2SchemaFix(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            List<String> badConstraints = jdbc.queryForList(
                    "SELECT tc.CONSTRAINT_NAME " +
                    "FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc " +
                    "JOIN INFORMATION_SCHEMA.CHECK_CONSTRAINTS cc " +
                    "  ON  tc.CONSTRAINT_CATALOG = cc.CONSTRAINT_CATALOG " +
                    "  AND tc.CONSTRAINT_SCHEMA  = cc.CONSTRAINT_SCHEMA " +
                    "  AND tc.CONSTRAINT_NAME    = cc.CONSTRAINT_NAME " +
                    "WHERE tc.TABLE_NAME      = 'USERS' " +
                    "  AND tc.CONSTRAINT_TYPE = 'CHECK' " +
                    "  AND cc.CHECK_CLAUSE NOT LIKE ?",
                    String.class,
                    "%'USER'%");

            for (String name : badConstraints) {
                jdbc.execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS \"" + name + "\"");
                log.warn("Dropped stale discriminator check constraint '{}' (did not include 'USER')", name);
            }
        } catch (Exception e) {
            log.debug("H2SchemaFix skipped: {}", e.getMessage());
        }
    }
}
