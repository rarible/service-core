package com.rarible.core.common

import org.slf4j.Logger
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.util.function.*
import java.util.*
import java.util.concurrent.Callable

fun <T> Mono<T>.subscribeAndLog(name: String, logger: Logger) {
    this.subscribe(
        {},
        { logger.error("error caught while: $name", it) },
        { logger.info("completed successfully: $name") },
        { logger.info("started: $name") }
    )
}

fun <T> T?.justOrEmpty(): Mono<T> {
    return Mono.justOrEmpty(this)
}

fun <T> Callable<T>.blockingToMono(): Mono<T> {
    return Mono.just(Unit)
        .publishOn(Schedulers.boundedElastic())
        .flatMap {
            try {
                Mono.just(this.call())
            } catch (e: Exception) {
                Mono.error<T>(e)
            }
        }
}

inline fun <reified R> Flux<*>.filterIsInstance(): Flux<R> {
    return this
        .filter { it is R }
        .map { it as R }
}

fun <T, R> Flux<T>.mapNotNull(mapper: (T) -> R?): Flux<R> {
    return this
        .flatMap {
            val r = mapper(it)
            if (r == null) {
                Mono.empty()
            } else {
                Mono.just(r)
            }
        }
}

fun <T> Mono<T>.toOptional(): Mono<Optional<T>> =
    this.map { Optional.of(it) }
        .switchIfEmpty(Mono.just(Optional.empty()))

fun <T> Mono<Optional<T>>.fromOptional(): Mono<T> =
    this.flatMap {
        if (it.isPresent) {
            Mono.just(it.get())
        } else {
            Mono.empty()
        }
    }

fun <T> Mono<Collection<T>>.toFlux(): Flux<T> =
    this.flatMapMany { Flux.fromIterable(it) }


operator fun <T1, T2> Tuple2<T1, T2>.component1(): T1 {
    return t1
}

operator fun <T1, T2> Tuple2<T1, T2>.component2(): T2 {
    return t2
}

operator fun <T1, T2, T3> Tuple3<T1, T2, T3>.component1(): T1 {
    return t1
}

operator fun <T1, T2, T3> Tuple3<T1, T2, T3>.component2(): T2 {
    return t2
}

operator fun <T1, T2, T3> Tuple3<T1, T2, T3>.component3(): T3 {
    return t3
}

operator fun <T1, T2, T3, T4> Tuple4<T1, T2, T3, T4>.component1(): T1 {
    return t1
}

operator fun <T1, T2, T3, T4> Tuple4<T1, T2, T3, T4>.component2(): T2 {
    return t2
}

operator fun <T1, T2, T3, T4> Tuple4<T1, T2, T3, T4>.component3(): T3 {
    return t3
}

operator fun <T1, T2, T3, T4> Tuple4<T1, T2, T3, T4>.component4(): T4 {
    return t4
}

operator fun <T1, T2, T3, T4, T5> Tuple5<T1, T2, T3, T4, T5>.component1(): T1 {
    return t1
}

operator fun <T1, T2, T3, T4, T5> Tuple5<T1, T2, T3, T4, T5>.component2(): T2 {
    return t2
}

operator fun <T1, T2, T3, T4, T5> Tuple5<T1, T2, T3, T4, T5>.component3(): T3 {
    return t3
}

operator fun <T1, T2, T3, T4, T5> Tuple5<T1, T2, T3, T4, T5>.component4(): T4 {
    return t4
}

operator fun <T1, T2, T3, T4, T5> Tuple5<T1, T2, T3, T4, T5>.component5(): T5 {
    return t5
}

operator fun <T1, T2, T3, T4, T5, T6> Tuple6<T1, T2, T3, T4, T5, T6>.component6(): T6 = t6

operator fun <T1, T2, T3, T4, T5, T6, T7> Tuple7<T1, T2, T3, T4, T5, T6, T7>.component7(): T7 = t7

operator fun <T1, T2, T3, T4, T5, T6, T7, T8> Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>.component8(): T8 = t8

operator fun <T1, T2, T3, T4, T5, T6, T7, T8, T9> Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>.component9(): T9 = t9

operator fun <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> Tuple10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>.component10(): T10 = t10

operator fun <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> Tuple11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>.component11(): T11 = t11

@Suppress("UNCHECKED_CAST")
fun <T1, T2, T3, T4, T5, T6, T7, T8, T9> zip(
    t1: Mono<T1>, t2: Mono<T2>, t3: Mono<T3>, t4: Mono<T4>, t5: Mono<T5>, t6: Mono<T6>, t7: Mono<T7>, t8: Mono<T8>, t9: Mono<T9>
): Mono<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> =
    reactor.kotlin.core.publisher.zip(t1, t2, t3, t4, t5, t6, t7, t8, t9) { zipped ->
        Tuple9(
            zipped[0] as T1,
            zipped[1] as T2,
            zipped[2] as T3,
            zipped[3] as T4,
            zipped[4] as T5,
            zipped[5] as T6,
            zipped[6] as T7,
            zipped[7] as T8,
            zipped[8] as T9
        )
    }

@Suppress("UNCHECKED_CAST")
fun <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> zip(
    t1: Mono<T1>, t2: Mono<T2>, t3: Mono<T3>, t4: Mono<T4>, t5: Mono<T5>, t6: Mono<T6>, t7: Mono<T7>, t8: Mono<T8>, t9: Mono<T9>, t10: Mono<T10>
): Mono<Tuple10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>> =
    reactor.kotlin.core.publisher.zip(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10) { zipped ->
        Tuple10(
            zipped[0] as T1,
            zipped[1] as T2,
            zipped[2] as T3,
            zipped[3] as T4,
            zipped[4] as T5,
            zipped[5] as T6,
            zipped[6] as T7,
            zipped[7] as T8,
            zipped[8] as T9,
            zipped[9] as T10
        )
    }

@Suppress("UNCHECKED_CAST")
fun <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> zip(
    t1: Mono<T1>, t2: Mono<T2>, t3: Mono<T3>, t4: Mono<T4>, t5: Mono<T5>, t6: Mono<T6>, t7: Mono<T7>, t8: Mono<T8>, t9: Mono<T9>, t10: Mono<T10>, t11: Mono<T11>
): Mono<Tuple11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>> =
    reactor.kotlin.core.publisher.zip(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11) { zipped ->
        Tuple11(
            zipped[0] as T1,
            zipped[1] as T2,
            zipped[2] as T3,
            zipped[3] as T4,
            zipped[4] as T5,
            zipped[5] as T6,
            zipped[6] as T7,
            zipped[7] as T8,
            zipped[8] as T9,
            zipped[9] as T10,
            zipped[10] as T11
        )
    }