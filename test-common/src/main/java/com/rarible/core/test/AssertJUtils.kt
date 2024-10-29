package com.rarible.core.test

import org.assertj.core.api.AbstractAssert
import org.assertj.core.api.AbstractIterableAssert
import org.assertj.core.api.AbstractListAssert
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Condition
import org.assertj.core.api.InstanceOfAssertFactory
import org.assertj.core.api.ListAssert
import org.assertj.core.api.ObjectAssert
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.reflect.KProperty1

inline fun <T, reified P, A : AbstractAssert<A, T>, PA : AbstractAssert<PA, P>> A.hasPropertyAssert(property: KProperty1<T, P>, crossinline createAssert: (P) -> PA, crossinline valueAssert: PA.() -> PA): A {
    val description = descriptionText()
    return satisfies(
        Condition({ value ->
            val assert = assertThat(value)
                .`as`("[$description]\nExtracting ${property.name} from $value")
                .extracting(
                    property.name,
                    InstanceOfAssertFactory(P::class.java) {
                        createAssert(it)
                    }
                )
            assert.valueAssert()
            true
        }, "")
    )
}

inline fun <T, reified P, A : AbstractAssert<A, T>> A.hasProperty(property: KProperty1<T, P>, crossinline valueAssert: ObjectAssert<P>.() -> ObjectAssert<P>) =
    hasPropertyAssert(property, createAssert = { assertThat(it) }, valueAssert = valueAssert)

inline fun <T, reified P, A : AbstractAssert<A, T>> A.hasProperty(property: KProperty1<T, P>, value: P) =
    hasPropertyAssert(property, createAssert = { assertThat(it) }, valueAssert = { eq(value) })

inline fun <T, reified P : List<PE>, PE, A : AbstractAssert<A, T>> A.hasListProperty(property: KProperty1<T, P>, crossinline valueAssert: ListAssert<PE>.() -> ListAssert<PE>) =
    hasPropertyAssert(property, createAssert = { assertThat(it) }, valueAssert = valueAssert)

fun <T, A : AbstractAssert<A, T>> A.eq(value: T): A = isEqualTo(value)

@Suppress("UNCHECKED_CAST")
val <T : Any> ObjectAssert<T?>.notNull: ObjectAssert<T>; get() = isNotNull as ObjectAssert<T>

fun <A : AbstractListAssert<A, L, E, EA>, L : List<E>, E, EA : AbstractAssert<EA, E>> A.assertElements(vararg elementAsserts: EA.() -> EA): A {
    elementAsserts.forEachIndexed { i, elementAssert ->
        element(i).elementAssert()
    }
    return this
}

fun <A : AbstractIterableAssert<A, I, E, EA>, I : Iterable<E>, E, EA : AbstractAssert<EA, E>, R>
A.extracting(f: (E) -> R, assertF: AbstractListAssert<*, List<R>, R, ObjectAssert<R>>.() -> Any): A {
    extracting(java.util.function.Function { f(it) }).assertF()
    return this
}

class TestHasProperty {
    @Test
    fun hasProperty() {
        data class C(val property: String, val otherProperty: Int = 42)

        val error = assertThrows<AssertionError> {
            assertThat(C("foo")).`as`("baz").hasProperty(C::property) { eq("bar") }
        }
        assertThat(error)
            .hasMessageContaining("baz")
            .hasMessageContaining("42")
    }

    @Test
    fun hasIntProperty() {
        data class C(val property: Int)

        assertThat(C(42)).hasFieldOrPropertyWithValue("property", 42)
        assertThat(C(42)).hasProperty(C::property) { isEqualTo(42) }
        val error = assertThrows<AssertionError> {
            assertThat(C(42)).hasProperty(C::property) { eq(5) }
        }
        assertThat(error)
            .hasMessageContaining("5")
            .hasMessageContaining("42")
    }
}

class TestAssertElements {
    @Test
    fun assertElements() {
        assertThat(listOf(C("A"), C("B"))).assertElements(
            { hasProperty(C::n) { eq("A") } },
            { hasProperty(C::n) { eq("B") } }
        )
        val error = assertThrows<AssertionError> {
            assertThat(listOf(C("A"), C("B"))).assertElements(
                { hasProperty(C::n) { eq("A") } },
                { hasProperty(C::n) { eq("_") } }
            )
        }
        assertThat(error)
            .hasMessageContaining("_")
            .hasMessageContaining("B")
            .hasMessageContaining("List element at index 1")
    }

    class C(val n: String)
}
