package com.mcporchestrator.infrastructure.adapter.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

import javax.sql.DataSource;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.mcporchestrator.application.port.output.ResourceRepository;
import com.mcporchestrator.domain.entity.MCPResource;

@Repository
@Profile("mysql")
public class DatabaseAdapterMysql implements ResourceRepository {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseAdapterMysql(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        initializeSchema();
    }

    private void initializeSchema() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS mcp_resources (
                    id VARCHAR(36) PRIMARY KEY,
                    name VARCHAR(500) NOT NULL,
                    type VARCHAR(100) NOT NULL,
                    content LONGTEXT NOT NULL,
                    metadata JSON DEFAULT ('{}'),
                    source VARCHAR(200),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    ttl_seconds BIGINT DEFAULT 0,
                    INDEX idx_name (name),
                    INDEX idx_type (type),
                    INDEX idx_source (source)
                )
                """);
    }

    @Override
    public MCPResource save(MCPResource resource) {
        String sql = """
                INSERT INTO mcp_resources (id, name, type, content, metadata, source, created_at, ttl_seconds)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    name = VALUES(name),
                    type = VALUES(type),
                    content = VALUES(content),
                    metadata = VALUES(metadata),
                    source = VALUES(source),
                    ttl_seconds = VALUES(ttl_seconds)
                """;

        String metadataJson = "{}";
        if (resource.metadata() != null && !resource.metadata().isEmpty()) {
            metadataJson = serializeMetadata(resource.metadata());
        }

        jdbcTemplate.update(sql,
                resource.id().toString(),
                resource.name(),
                resource.type(),
                resource.content(),
                metadataJson,
                resource.source(),
                resource.createdAt() != null ? Timestamp.from(resource.createdAt()) : Timestamp.from(Instant.now()),
                resource.ttlSeconds()
        );

        return findById(resource.id()).orElse(resource);
    }

    @Override
    public Optional<MCPResource> findById(UUID id) {
        List<MCPResource> results = jdbcTemplate.query(
                "SELECT * FROM mcp_resources WHERE id = ?",
                new MCPResourceRowMapper(),
                id.toString()
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public List<MCPResource> search(String query, String type, String source) {
        StringBuilder sql = new StringBuilder("SELECT * FROM mcp_resources WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (query != null && !query.isBlank()) {
            sql.append(" AND (content LIKE ? OR name LIKE ?)");
            params.add("%" + query + "%");
            params.add("%" + query + "%");
        }
        if (type != null && !type.isBlank()) {
            sql.append(" AND type = ?");
            params.add(type);
        }
        if (source != null && !source.isBlank()) {
            sql.append(" AND source = ?");
            params.add(source);
        }
        sql.append(" ORDER BY created_at DESC LIMIT 100");

        return jdbcTemplate.query(sql.toString(), new MCPResourceRowMapper(), params.toArray());
    }

    @Override
    public List<MCPResource> findAll() {
        return jdbcTemplate.query(
                "SELECT * FROM mcp_resources ORDER BY created_at DESC LIMIT 1000",
                new MCPResourceRowMapper()
        );
    }

    @Override
    public void deleteById(UUID id) {
        jdbcTemplate.update("DELETE FROM mcp_resources WHERE id = ?", id.toString());
    }

    @Override
    public boolean existsById(UUID id) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM mcp_resources WHERE id = ?",
                Integer.class,
                id.toString()
        );
        return count != null && count > 0;
    }

    private String serializeMetadata(Map<String, String> metadata) {
        StringBuilder json = new StringBuilder("{");
        Iterator<Map.Entry<String, String>> it = metadata.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();
            json.append("\"").append(escapeJson(entry.getKey())).append("\":\"")
                    .append(escapeJson(entry.getValue())).append("\"");
            if (it.hasNext()) {
                json.append(",");
            }
        }
        json.append("}");
        return json.toString();
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static final class MCPResourceRowMapper implements RowMapper<MCPResource> {
        @Override
        public MCPResource mapRow(ResultSet rs, int rowNum) throws SQLException {
            Map<String, String> metadata = new HashMap<>();
            String metaJson = rs.getString("metadata");
            if (metaJson != null && !metaJson.equals("{}")) {
                metadata = parseJsonMetadata(metaJson);
            }

            Timestamp ts = rs.getTimestamp("created_at");

            return new MCPResource(
                    UUID.fromString(rs.getString("id")),
                    rs.getString("name"),
                    rs.getString("type"),
                    rs.getString("content"),
                    metadata,
                    rs.getString("source"),
                    ts != null ? ts.toInstant() : Instant.now(),
                    rs.getLong("ttl_seconds")
            );
        }

        private Map<String, String> parseJsonMetadata(String json) {
            Map<String, String> result = new HashMap<>();
            if (json == null || json.isBlank() || json.equals("{}")) {
                return result;
            }
            String inner = json.trim();
            if (inner.startsWith("{") && inner.endsWith("}")) {
                inner = inner.substring(1, inner.length() - 1).trim();
            }
            String[] pairs = inner.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
            for (String pair : pairs) {
                String[] kv = pair.split(":(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", 2);
                if (kv.length == 2) {
                    String key = kv[0].trim().replaceAll("^\"|\"$", "");
                    String value = kv[1].trim().replaceAll("^\"|\"$", "");
                    result.put(key, value);
                }
            }
            return result;
        }
    }
}
