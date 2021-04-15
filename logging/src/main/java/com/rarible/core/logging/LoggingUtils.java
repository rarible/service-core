package com.rarible.core.logging;

import kotlinx.coroutines.slf4j.MDCContext;
import net.logstash.logback.marker.Markers;
import org.slf4j.Marker;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.Collections;
import java.util.function.Function;

public class LoggingUtils {
    private static final String CONTEXT_NAME = MDCContext.class.getSimpleName();
    private static final Marker EMPTY_MARKER = Markers.appendEntries(Collections.emptyMap());

    public static <T> Mono<T> withMarker(Function<Marker, Mono<T>> action) {
        return Mono.subscriberContext()
            .map(LoggingUtils::createMarker)
            .flatMap(action);
    }
    
    public static <T> Flux<T> withMarkerFlux(Function<Marker, Flux<T>> action) {
        return Mono.subscriberContext()
            .map(LoggingUtils::createMarker)
            .flatMapMany(action);
    }

    public static Mono<Marker> marker() {
        return Mono.subscriberContext()
            .map(LoggingUtils::createMarker);
    }

    private static Marker createMarker(Context ctx) {
        if (ctx.hasKey(CONTEXT_NAME)) {
            return Markers.appendEntries(ctx.<MDCContext>get(CONTEXT_NAME).getContextMap());
        } else {
            return EMPTY_MARKER;
        }
    }
}
