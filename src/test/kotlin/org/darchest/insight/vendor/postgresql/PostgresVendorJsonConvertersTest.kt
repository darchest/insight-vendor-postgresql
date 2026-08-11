package org.darchest.insight.vendor.postgresql

import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PostgresVendorJsonConvertersTest {

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

		val preferredNames = listOf("getConverter", "converter", "findConverter")
		for (name in preferredNames) {
			val m = registryClass.methods.firstOrNull { it.name == name && it.parameterTypes.size == 2 }
			if (m != null) return m.invoke(registryInstance, javaClass, sqlTypeClass)
		}

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
	fun json_toSql_objectNullAndJsonNull() {
		val obj = JsonObject().apply { addProperty("a", 1) }
		assertEquals("'{\"a\":1}'", javaToSql(JsonElement::class.java, JsonType::class.java, obj))
		assertEquals("'null'", javaToSql(JsonElement::class.java, JsonType::class.java, JsonNull.INSTANCE))
		assertEquals("NULL", javaToSql(JsonElement::class.java, JsonType::class.java, null))
	}

	@Test
	fun jsonb_toSql_objectNullAndJsonNull() {
		val obj = JsonParser.parseString("""{"name":"O'Brien"}""")
		assertEquals("'{\"name\":\"O''Brien\"}'", javaToSql(JsonElement::class.java, JsonbType::class.java, obj))
		assertEquals("'null'", javaToSql(JsonElement::class.java, JsonbType::class.java, JsonNull.INSTANCE))
		assertEquals("NULL", javaToSql(JsonElement::class.java, JsonbType::class.java, null))
	}
}
