package com.rarible.core.test.mongo;

import com.mongodb.client.result.DeleteResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

public class AsyncMongoHelper {
    @Autowired(required = false)
    private ReactiveMongoOperations mongo;

    private final List<String> ignoreCollections = Arrays.asList(
        "db_version",
        "counters",
        "mongockChangeLog",
        "mongockLock"
    );

    public Flux<DeleteResult> cleanup() {
        if (mongo != null) {
            return mongo.getCollectionNames()
                .filter(this::filterCollection)
                .flatMap(this::cleanupCollection);
        } else {
            return Flux.empty();
        }
    }

    private boolean filterCollection(String name) {
        return !ignoreCollections.contains(name) && !name.startsWith("system");
    }

    public Mono<DeleteResult> cleanupCollection(String collectionName) {
        return mongo.remove(new Query(), collectionName);
    }
}
