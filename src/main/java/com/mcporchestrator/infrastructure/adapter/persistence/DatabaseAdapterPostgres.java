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
@Profile("postgres")
public class DatabaseAdapterPostgres implements ResourceRepository {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseAdapterPostgres(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        initializeSchema();
    }

    private void initializeSchema() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS mcp_resources (
                    id UUID PRIMARY KEY,
                    name VARCHAR(500) NOT NULL,
                    type VARCHAR(100) NOT NULL,
                    content TEXT NOT NULL,
                    metadata JSONB DEFAULT '{}',
                    source VARCHAR(200),
                    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                    ttl_seconds BIGINT DEFAULT 0
                )
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_mcp_resources_name
                ON mcp_resources (name)
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_mcp_resources_type
                ON mcp_resources (type)
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_mcp_resources_source
                ON mcp_resources (source)
                """);
    }

    @Override
    public MCPResource save(MCPResource resource) {
        String sql = """
                INSERT INTO mcp_resources (id, name, type, content, metadata, source, created_at, ttl_seconds)
                VALUES (?, ?, ?, ?, ?::jsonb, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    name = EXCLUDED.name,
                    type = EXCLUDED.type,
                    content = EXCLUDED.content,
                    metadata = EXCLUDED.metadata,
                    source = EXCLUDED.source,
                    ttl_seconds = EXCLUDED.ttl_seconds
                """;

        String metadataJson = "{}";
        if (resource.metadata() != null && !resource.metadata().isEmpty()) {
            metadataJson = serializeMetadata(resource.metadata());
        }

        jdbcTemplate.update(sql,
                resource.id(),
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
                id
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public List<MCPResource> search(String query, String type, String source) {
        StringBuilder sql = new StringBuilder("SELECT * FROM mcp_resources WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (query != null && !query.isBlank()) {
            sql.append(" AND (to_tsvector('english', content) @@ plainto_tsquery('english', ?)");
            sql.append(" OR name ILIKE ?)");
            params.add(query);
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
        jdbcTemplate.update("DELETE FROM mcp_resources WHERE id = ?", id);
    }

    @Override
    public boolean existsById(UUID id) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM mcp_resources WHERE id = ?",
                Integer.class,
                id
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
