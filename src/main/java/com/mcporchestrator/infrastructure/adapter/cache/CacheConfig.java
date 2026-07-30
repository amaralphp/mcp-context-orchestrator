package com.mcporchestrator.infrastructure.adapter.cache;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.mcporchestrator.application.port.output.CacheService;
import com.mcporchestrator.domain.entity.MCPResource;

@Configuration
public class CacheConfig implements CacheService {

    private final ConcurrentHashMap<UUID, CacheEntry<MCPResource>> resourceCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CacheEntry<List<MCPResource>>> searchCache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
    private final long ttlSeconds;

    public CacheConfig(@Value("${mcp.cache.ttl-seconds:300}") long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
        cleanupExecutor.scheduleAtFixedRate(
                this::evictExpired,
                30,
                30,
                TimeUnit.SECONDS
        );
    }

    @Override
    public void cacheResource(MCPResource resource) {
        resourceCache.put(
                resource.id(),
                new CacheEntry<>(resource, ttlDuration())
        );
    }

    @Override
    public Optional<MCPResource> getResource(UUID id) {
        CacheEntry<MCPResource> entry = resourceCache.get(id);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.isExpired()) {
            resourceCache.remove(id);
            return Optional.empty();
        }
        return Optional.of(entry.value());
    }

    @Override
    public void invalidateResource(UUID id) {
        resourceCache.remove(id);
    }

    @Override
    public void cacheSearchResults(String queryKey, List<MCPResource> results) {
        searchCache.put(queryKey, new CacheEntry<>(results, ttlDuration()));
    }

    @Override
    public Optional<List<MCPResource>> getSearchResults(String queryKey) {
        CacheEntry<List<MCPResource>> entry = searchCache.get(queryKey);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.isExpired()) {
            searchCache.remove(queryKey);
            return Optional.empty();
        }
        return Optional.of(entry.value());
    }

    @Override
    public void clearAll() {
        resourceCache.clear();
        searchCache.clear();
    }

    public long size() {
        return resourceCache.size() + searchCache.size();
    }

    private Duration ttlDuration() {
        return Duration.ofSeconds(ttlSeconds);
    }

    private void evictExpired() {
        resourceCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        searchCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private record CacheEntry<T>(T value, Instant expiresAt) {
        CacheEntry(T value, Duration ttl) {
            this(value, Instant.now().plus(ttl));
        }

        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
