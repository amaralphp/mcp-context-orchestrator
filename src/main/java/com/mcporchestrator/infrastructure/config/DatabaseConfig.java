package com.mcporchestrator.infrastructure.config;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

import com.zaxxer.hikari.HikariDataSource;

@Configuration
public class DatabaseConfig {

    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.datasource.url", matchIfMissing = false)
    public DataSource dataSource(
            org.springframework.core.env.Environment env
    ) {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(env.getProperty("spring.datasource.url"));
        ds.setUsername(env.getProperty("spring.datasource.username"));
        ds.setPassword(env.getProperty("spring.datasource.password"));
        ds.setDriverClassName(env.getProperty("spring.datasource.driver-class-name",
                "org.postgresql.Driver"));
        return ds;
    }

    @Bean
    @ConditionalOnProperty(name = "spring.data.mongodb.uri", matchIfMissing = false)
    public MongoDatabaseFactory mongoDatabaseFactory(
            org.springframework.core.env.Environment env
    ) {
        String uri = env.getProperty("spring.data.mongodb.uri");
        if (uri != null) {
            return new SimpleMongoClientDatabaseFactory(uri);
        }
        return new SimpleMongoClientDatabaseFactory(
                "mongodb://localhost:27017/mcporch"
        );
    }

    @Bean
    @ConditionalOnProperty(name = "spring.data.mongodb.uri", matchIfMissing = false)
    public MongoTemplate mongoTemplate(MongoDatabaseFactory factory) {
        return new MongoTemplate(factory);
    }
}
