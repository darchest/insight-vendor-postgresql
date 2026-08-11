package org.darchest.insight.vendor.postgresql

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class PostgresVendorDateTimeConvertersTest {

	companion object {
		private var initialized = false

		private fun ensureInit() {
			if (initialized) return
			PostgresVendor.init()
			initialized = true
		}
	}

	@BeforeTest
	fun setup() {
		ensureInit()
	}

	private fun converter(javaClass: Class<*>, sqlTypeClass: Class<*>): Any {
		val registryClass = Class.forName("org.darchest.insight.SqlTypeConvertersRegistry")
		val registryInstance = registryClass.declaredFields.firstOrNull { it.name == "INSTANCE" }?.let { f ->
			f.isAccessible = true
			f.get(null)
		}

		// Prefer a stable public API if present
		val preferredNames = listOf("getConverter", "converter", "findConverter")
		for (name in preferredNames) {
			val m = registryClass.methods.firstOrNull { it.name == name && it.parameterTypes.size == 2 }
			if (m != null) return m.invoke(registryInstance, javaClass, sqlTypeClass)
		}

		// Otherwise: heuristic search for a 2-arg method that looks like a converter getter
		val m = registryClass.methods.firstOrNull { method ->
			method.parameterTypes.size == 2 &&
				method.parameterTypes[0] == Class::class.java &&
				method.parameterTypes[1] == Class::class.java &&
				method.name.contains("converter", ignoreCase = true)
		}
		if (m != null) return m.invoke(registryInstance, javaClass, sqlTypeClass)

		val methods = registryClass.methods
			.sortedBy { it.name }
			.joinToString("\n") { it.toString() }
		throw IllegalStateException(
			"Cannot locate SqlTypeConvertersRegistry converter getter. Available methods:\n$methods"
		)
	}

	private fun javaToSql(javaClass: Class<*>, sqlTypeClass: Class<*>, value: Any?): String {
		val conv = converter(javaClass, sqlTypeClass)
		val m = conv.javaClass.methods.firstOrNull { it.name == "javaToSql" && it.parameterTypes.size == 1 }
			?: throw IllegalStateException("Converter ${conv.javaClass.name} has no javaToSql(value) method")
		return m.invoke(conv, value) as String
	}

	@Test
	fun localDate_toSql_infinityAndNull() {
		assertEquals("'-infinity'", javaToSql(LocalDate::class.java, DateType::class.java, LocalDate.MIN))
		assertEquals("'infinity'", javaToSql(LocalDate::class.java, DateType::class.java, LocalDate.MAX))
		assertEquals("'2020-01-02'", javaToSql(LocalDate::class.java, DateType::class.java, LocalDate.of(2020, 1, 2)))
		assertEquals("NULL", javaToSql(LocalDate::class.java, DateType::class.java, null))
	}

	@Test
	fun localTime_toSql_basicAndNull() {
		assertEquals("'00:00'", javaToSql(LocalTime::class.java, TimeType::class.java, LocalTime.MIDNIGHT))
		assertEquals("'12:34:56'", javaToSql(LocalTime::class.java, TimeType::class.java, LocalTime.of(12, 34, 56)))
		assertEquals("NULL", javaToSql(LocalTime::class.java, TimeType::class.java, null))
	}

	@Test
	fun localDateTime_toSql_infinityAndNull() {
		assertEquals("'-infinity'", javaToSql(LocalDateTime::class.java, TimeStampType::class.java, LocalDateTime.MIN))
		assertEquals("'infinity'", javaToSql(LocalDateTime::class.java, TimeStampType::class.java, LocalDateTime.MAX))
		assertEquals(
			"'2020-01-02T03:04:05'",
			javaToSql(LocalDateTime::class.java, TimeStampType::class.java, LocalDateTime.of(2020, 1, 2, 3, 4, 5))
		)
		assertEquals("NULL", javaToSql(LocalDateTime::class.java, TimeStampType::class.java, null))
	}

	@Test
	fun instant_toSql_infinityAndNull() {
		assertEquals("'-infinity'", javaToSql(Instant::class.java, TimeStampWithTimeZoneType::class.java, Instant.MIN))
		assertEquals("'infinity'", javaToSql(Instant::class.java, TimeStampWithTimeZoneType::class.java, Instant.MAX))
		assertEquals("'1970-01-01T00:00:00Z'", javaToSql(Instant::class.java, TimeStampWithTimeZoneType::class.java, Instant.EPOCH))
		assertEquals("NULL", javaToSql(Instant::class.java, TimeStampWithTimeZoneType::class.java, null))
	}
}

