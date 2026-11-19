package com.springfieldarchive.sync;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class SyncStateRepository {

    private static final RowMapper<SyncState> MAPPER = (rs, rowNum) -> new SyncState(
            rs.getString("resource"),
            rs.getString("status"),
            rs.getInt("last_page"),
            rs.getInt("processed"),
            rs.getInt("errors"),
            rs.getObject("synced_at", OffsetDateTime.class),
            rs.getObject("updated_at", OffsetDateTime.class),
            rs.getString("error"));

    private final NamedParameterJdbcTemplate jdbc;

    public SyncStateRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<SyncState> findAll() {
        return jdbc.query("SELECT * FROM sync_state ORDER BY resource", MAPPER);
    }

    public Optional<SyncState> find(String resource) {
        List<SyncState> rows = jdbc.query("SELECT * FROM sync_state WHERE resource = :r",
                new MapSqlParameterSource("r", resource), MAPPER);
        return rows.stream().findFirst();
    }

    public void markRunning(String resource) {
        jdbc.update("UPDATE sync_state SET status='running', error=NULL, updated_at=now() WHERE resource = :r",
                new MapSqlParameterSource("r", resource));
    }

    public void markIdle(String resource) {
        jdbc.update("UPDATE sync_state SET status='idle', updated_at=now() WHERE resource = :r",
                new MapSqlParameterSource("r", resource));
    }

    public void markCompleted(String resource, int processed, int errors) {
        jdbc.update("UPDATE sync_state SET status='completed', processed = processed + :n, errors = errors + :e,"
                        + " last_page = 0, synced_at = now(), updated_at = now(), error = NULL WHERE resource = :r",
                new MapSqlParameterSource("r", resource).addValue("n", processed).addValue("e", errors));
    }

    public void markFailed(String resource, String error) {
        jdbc.update("UPDATE sync_state SET status='failed', error = :err, updated_at = now() WHERE resource = :r",
                new MapSqlParameterSource("r", resource).addValue("err", error));
    }

    public void saveProgress(String resource, int lastPage) {
        jdbc.update("UPDATE sync_state SET last_page = :page, updated_at = now() WHERE resource = :r",
                new MapSqlParameterSource("r", resource).addValue("page", lastPage));
    }
}
