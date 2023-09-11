package com.rarible.core.test.data

import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.RandomUtils
import scalether.domain.AddressFactory
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.random.Random

fun randomBoolean() = RandomUtils.nextBoolean()

fun randomSign(): Int = if (randomBoolean()) 1 else -1

fun randomInt() = randomInt(Int.MAX_VALUE)
fun randomInt(endExc: Int) = randomInt(0, endExc)
fun randomInt(startInc: Int, endExc: Int) = RandomUtils.nextInt(startInc, endExc)

fun randomIntSigned() = randomIntSigned(Int.MAX_VALUE)
fun randomIntSigned(endExc: Int) = randomIntSigned(0, endExc)
fun randomIntSigned(startInc: Int, endExc: Int) = Random.nextInt(startInc, endExc)

fun randomLong() = randomLong(Long.MAX_VALUE)
fun randomLong(endExc: Long) = randomLong(0, endExc)
fun randomLong(startInc: Long, endExc: Long) = RandomUtils.nextLong(startInc, endExc)

fun randomLongSigned() = randomLongSigned(Long.MAX_VALUE)
fun randomLongSigned(endExc: Long) = randomLongSigned(0, endExc)
fun randomLongSigned(startInc: Long, endExc: Long) = Random.nextLong(startInc, endExc)

fun randomDouble() = randomDouble(Double.MAX_VALUE)
fun randomDouble(endExc: Double) = randomDouble(0.0, endExc)
fun randomDouble(startInc: Double, endExc: Double) = RandomUtils.nextDouble(startInc, endExc)

fun randomDoubleSigned() = randomDoubleSigned(Double.MAX_VALUE)
fun randomDoubleSigned(endExc: Double) = randomDoubleSigned(0.0, endExc)
fun randomDoubleSigned(startInc: Double, endExc: Double) = Random.nextDouble(startInc, endExc)

fun randomString() = randomString(8)
fun randomString(length: Int) = RandomStringUtils.randomAlphabetic(length)!!

fun randomBigInt() = randomBigInt(32)
fun randomBigInt(length: Int) = BigInteger(RandomStringUtils.randomNumeric(length))
fun randomBigIntSigned(length: Int): BigInteger {
    val result = randomBigInt(length)
    return if (randomBoolean()) result else result.negate()
}

fun randomBigDecimal() = randomBigDecimal(24, 8)
fun randomBigDecimal(intLength: Int, decimalLength: Int): BigDecimal {
    val integerPart = RandomStringUtils.randomNumeric(intLength)
    val decimalPart = RandomStringUtils.randomNumeric(decimalLength)
    return BigDecimal("$integerPart.$decimalPart")
}

fun randomBigDecimalSigned(intLength: Int, decimalLength: Int): BigDecimal {
    val result = randomBigDecimal(intLength, decimalLength)
    return if (randomBoolean()) result else result.negate()
}

fun randomBytes() = randomBytes(32)
fun randomBytes(size: Int) = RandomUtils.nextBytes(size)

fun randomByteArray() = randomByteArray(32)
fun randomByteArray(size: Int) = Random.nextBytes(size)

fun randomAddress() = AddressFactory.create()

fun randomBinary() = randomBinary(64)
fun randomBinary(length: Int) = Binary.apply(Random.nextBytes(length))

fun randomWord() = Word(Random.nextBytes(32)).toString()
