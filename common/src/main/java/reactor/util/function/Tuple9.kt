package reactor.util.function

import reactor.util.annotation.NonNull
import reactor.util.annotation.Nullable
import java.util.*
import java.util.function.Function

open class Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9> constructor(t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6, t7: T7, t8: T8, t9: T9) : Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>(t1, t2, t3, t4, t5, t6, t7, t8) {
    @NonNull
    val t9: T9 = Objects.requireNonNull(t9, "t9")

    override fun <R> mapT1(mapper: Function<T1, R>): Tuple9<R, T2, T3, T4, T5, T6, T7, T8, T9> =
        Tuple9(mapper.apply(t1), t2, t3, t4, t5, t6, t7, t8, t9)

    override fun <R> mapT2(mapper: Function<T2, R>): Tuple9<T1, R, T3, T4, T5, T6, T7, T8, T9> =
        Tuple9(t1, mapper.apply(t2), t3, t4, t5, t6, t7, t8, t9)

    override fun <R> mapT3(mapper: Function<T3, R>): Tuple9<T1, T2, R, T4, T5, T6, T7, T8, T9> =
        Tuple9(t1, t2, mapper.apply(t3), t4, t5, t6, t7, t8, t9)

    override fun <R> mapT4(mapper: Function<T4, R>): Tuple9<T1, T2, T3, R, T5, T6, T7, T8, T9> =
        Tuple9(t1, t2, t3, mapper.apply(t4), t5, t6, t7, t8, t9)

    override fun <R> mapT5(mapper: Function<T5, R>): Tuple9<T1, T2, T3, T4, R, T6, T7, T8, T9> =
        Tuple9(t1, t2, t3, t4, mapper.apply(t5), t6, t7, t8, t9)

    override fun <R> mapT6(mapper: Function<T6, R>): Tuple9<T1, T2, T3, T4, T5, R, T7, T8, T9> =
        Tuple9(t1, t2, t3, t4, t5, mapper.apply(t6), t7, t8, t9)

    override fun <R> mapT7(mapper: Function<T7, R>): Tuple9<T1, T2, T3, T4, T5, T6, R, T8, T9> =
        Tuple9(t1, t2, t3, t4, t5, t6, mapper.apply(t7), t8, t9)

    override fun <R> mapT8(mapper: Function<T8, R>): Tuple9<T1, T2, T3, T4, T5, T6, T7, R, T9> =
        Tuple9(t1, t2, t3, t4, t5, t6, t7, mapper.apply(t8), t9)

    open fun <R> mapT9(mapper: Function<T9, R>): Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, R> =
        Tuple9(t1, t2, t3, t4, t5, t6, t7, t8, mapper.apply(t9))

    @Nullable
    override fun get(index: Int): Any? =
        when (index) {
            0 -> t1
            1 -> t2
            2 -> t3
            3 -> t4
            4 -> t5
            5 -> t6
            6 -> t7
            7 -> t8
            8 -> t9
            else -> null
        }

    override fun toArray(): Array<Any?> = arrayOf(t1, t2, t3, t4, t5, t6, t7, t8, t9)

    override fun equals(@Nullable other: Any?): Boolean =
        if (this === other) {
            true
        } else if (other !is Tuple9<*, *, *, *, *, *, *, *, *>) {
            false
        } else if (!super.equals(other)) {
            false
        } else {
            t9 == other.t9
        }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + t9.hashCode()
        return result
    }

    override fun size(): Int = 9
}
