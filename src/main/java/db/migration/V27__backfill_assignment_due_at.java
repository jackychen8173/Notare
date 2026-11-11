package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Fills assignments.due_at (added by V26) from the old date-only due_date: 11:59 PM that day in
 * the course's time zone. Done in Java rather than SQL so the conversion uses the JVM's time zone
 * rules, the same ones the app uses for every later read and write. Postgres carries its own copy
 * of the rules, and the two disagreed about BC after it moved to permanent UTC-7 in 2026.
 */
public class V27__backfill_assignment_due_at extends BaseJavaMigration {

    private static final LocalTime END_OF_DAY = LocalTime.of(23, 59);

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        try (Statement select = connection.createStatement();
             ResultSet rows = select.executeQuery("""
                     SELECT a.id, a.due_date, c.time_zone
                     FROM assignments a
                     JOIN courses c ON c.id = a.course_id
                     WHERE a.due_at IS NULL""");
             PreparedStatement update = connection.prepareStatement(
                     "UPDATE assignments SET due_at = ? WHERE id = ?")) {
            int batched = 0;
            while (rows.next()) {
                LocalDate dueDate = rows.getObject("due_date", LocalDate.class);
                ZoneId zone = ZoneId.of(rows.getString("time_zone"));
                OffsetDateTime dueAt = dueDate.atTime(END_OF_DAY).atZone(zone).toInstant().atOffset(ZoneOffset.UTC);
                update.setObject(1, dueAt);
                update.setObject(2, rows.getObject("id", UUID.class));
                update.addBatch();
                if (++batched % 500 == 0) {
                    update.executeBatch();
                }
            }
            update.executeBatch();
        }

        try (Statement ddl = connection.createStatement()) {
            ddl.execute("ALTER TABLE assignments ALTER COLUMN due_at SET NOT NULL");
        }
    }
}
