package com.mcporchestrator.application.port.input;

import java.util.List;

import com.mcporchestrator.domain.entity.ContextQuery;
import com.mcporchestrator.domain.entity.QueryResult;

public interface QueryUseCase {
    List<QueryResult> executeQuery(ContextQuery contextQuery);
    String buildContextString(ContextQuery contextQuery);
}
