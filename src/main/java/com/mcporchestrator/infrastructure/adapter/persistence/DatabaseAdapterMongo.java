package com.mcporchestrator.infrastructure.adapter.persistence;

import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.bson.Document;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.mcporchestrator.application.port.output.ResourceRepository;
import com.mcporchestrator.domain.entity.MCPResource;

@Repository
@Profile("mongo")
public class DatabaseAdapterMongo implements ResourceRepository {

    private static final String COLLECTION = "mcp_resources";

    private final MongoTemplate mongoTemplate;

    public DatabaseAdapterMongo(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
        initializeIndexes();
    }

    private void initializeIndexes() {
        var asc = org.springframework.data.domain.Sort.Direction.ASC;
        var desc = org.springframework.data.domain.Sort.Direction.DESC;
        mongoTemplate.indexOps(COLLECTION).ensureIndex(new Index().on("name", asc));
        mongoTemplate.indexOps(COLLECTION).ensureIndex(new Index().on("type", asc));
        mongoTemplate.indexOps(COLLECTION).ensureIndex(new Index().on("source", asc));
        mongoTemplate.indexOps(COLLECTION).ensureIndex(new Index().on("created_at", desc));
    }

    @Override
    public MCPResource save(MCPResource resource) {
        Document doc = toDocument(resource);
        Document filter = new Document("_id", resource.id().toString());
        mongoTemplate.getCollection(COLLECTION).replaceOne(filter, doc,
                new com.mongodb.client.model.ReplaceOptions().upsert(true));
        return findById(resource.id()).orElse(resource);
    }

    @Override
    public Optional<MCPResource> findById(UUID id) {
        Document doc = mongoTemplate.getCollection(COLLECTION)
                .find(new Document("_id", id.toString()))
                .first();
        return Optional.ofNullable(doc).map(this::fromDocument);
    }

    @Override
    public List<MCPResource> search(String query, String type, String source) {
        List<Criteria> criteriaList = new ArrayList<>();

        if (query != null && !query.isBlank()) {
            String escaped = Pattern.quote(query);
            Criteria contentRegex = Criteria.where("content").regex(escaped, "i");
            Criteria nameRegex = Criteria.where("name").regex(escaped, "i");
            criteriaList.add(new Criteria().orOperator(contentRegex, nameRegex));
        }
        if (type != null && !type.isBlank()) {
            criteriaList.add(Criteria.where("type").is(type));
        }
        if (source != null && !source.isBlank()) {
            criteriaList.add(Criteria.where("source").is(source));
        }

        Query mongoQuery;
        if (criteriaList.isEmpty()) {
            mongoQuery = new Query();
        } else {
            mongoQuery = new Query(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }
        mongoQuery.limit(100);

        return mongoTemplate.find(mongoQuery, Document.class, COLLECTION)
                .stream()
                .map(this::fromDocument)
                .collect(Collectors.toList());
    }

    @Override
    public List<MCPResource> findAll() {
        return mongoTemplate.findAll(Document.class, COLLECTION)
                .stream()
                .map(this::fromDocument)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        mongoTemplate.getCollection(COLLECTION)
                .deleteOne(new Document("_id", id.toString()));
    }

    @Override
    public boolean existsById(UUID id) {
        return mongoTemplate.getCollection(COLLECTION)
                .countDocuments(new Document("_id", id.toString())) > 0;
    }

    private Document toDocument(MCPResource resource) {
        Document doc = new Document("_id", resource.id().toString())
                .append("name", resource.name())
                .append("type", resource.type())
                .append("content", resource.content())
                .append("source", resource.source())
                .append("ttl_seconds", resource.ttlSeconds());

        if (resource.createdAt() != null) {
            doc.append("created_at", Date.from(resource.createdAt()));
        } else {
            doc.append("created_at", Date.from(Instant.now()));
        }
        if (resource.metadata() != null) {
            doc.append("metadata", new Document(resource.metadata()));
        } else {
            doc.append("metadata", new Document());
        }
        return doc;
    }

    @SuppressWarnings("unchecked")
    private MCPResource fromDocument(Document doc) {
        Map<String, String> metadata = new HashMap<>();
        Document metaDoc = doc.get("metadata", Document.class);
        if (metaDoc != null) {
            for (Map.Entry<String, Object> entry : metaDoc.entrySet()) {
                metadata.put(entry.getKey(), entry.getValue() != null ? entry.getValue().toString() : "");
            }
        }

        Date created = doc.getDate("created_at");

        return new MCPResource(
                UUID.fromString(doc.getString("_id")),
                doc.getString("name"),
                doc.getString("type"),
                doc.getString("content"),
                metadata,
                doc.getString("source"),
                created != null ? created.toInstant() : Instant.now(),
                doc.getLong("ttl_seconds") != null ? doc.getLong("ttl_seconds") : 0
        );
    }
}
