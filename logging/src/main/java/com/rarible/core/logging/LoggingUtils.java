package com.rarible.core.logging;

import net.logstash.logback.marker.Markers;
import org.slf4j.Marker;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LoggingUtils {
    public static final String LOG_ = "__log_";
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
        final Map<String, String> mdcMap = extractMDCMap(ctx);
        if (!mdcMap.isEmpty()) {
            return Markers.appendEntries(mdcMap);
        } else {
            return EMPTY_MARKER;
        }
    }

    public static Map<String, String> extractMDCMap(Context ctx) {
        return ctx.stream()
            .filter(it -> it.getValue() instanceof String && it.getKey() instanceof String && ((String) it.getKey()).startsWith(LOG_))
            .collect(Collectors.toMap(it -> it.getKey().toString().substring(LOG_.length()), it -> it.getValue().toString()));
    }
}
