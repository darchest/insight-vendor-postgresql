/*
 * Copyright 2021-2024, Darchest and contributors.
 * Licensed under the Apache License, Version 2.0
 */

package org.darchest.insight.vendor.postgresql

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import org.darchest.insight.*
import org.postgresql.util.PGobject
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.sql.Types
import java.time.*
import java.util.*

object PostgresVendor: Vendor {

	private var inited = false

	abstract class DefaultNullTypeConverter: SqlTypeConverter {
		override fun javaToSql(value: Any?): String {
			if (value == null)
				return "NULL"
			return notNullJavaToSql(value)
		}

		override fun javaToPreparedSql(ps: PreparedStatement, ind: Int, value: Any?) {
			if (value == null) {
				ps.setNull(ind, 1)
				return
			}
			notNullJavaToPreparedSql(ps, ind, value)
		}

		abstract fun notNullJavaToSql(value: Any): String

		abstract fun notNullJavaToPreparedSql(ps: PreparedStatement, ind: Int, value: Any)
	}

	abstract class DateAndTimeNullTypeConverter : DefaultNullTypeConverter() {
		val negativeInfinity = "-infinity"
		val positiveInfinity = "infinity"
	}

	override fun init() {
		if (inited)
			return
		inited = true

		initUuidType()
		initCharTypes()
		initNumberTypes()
		initByteaTypes()
		initUuidArrayType()
		initBooleanType()
		initDateType()
		initTimeType()
		initTimeStampType()
		initTimeStampWithTimeZoneType()
		initJsonTypes()
	}

	private fun initUuidType() {
		SqlTypeConvertersRegistry.registerConverter(UUID::class.java, UuidType::class.java, object: DefaultNullTypeConverter() {
			override fun notNullJavaToSql(value: Any): String = "'$value'"

			override fun notNullJavaToPreparedSql(ps: PreparedStatement, ind: Int, value: Any) = ps.setObject(ind, value, Types.OTHER)

			override fun sqlToJava(rs: ResultSet, ind: Int): Any? = rs.getObject(ind, UUID::class.java)
		})
		SqlTypeConvertersRegistry.registerConverter(String::class.java, UuidType::class.java, object: DefaultNullTypeConverter() {
			override fun notNullJavaToSql(value: Any): String = "'${UUID.fromString(value as String)}'"

			override fun notNullJavaToPreparedSql(ps: PreparedStatement, ind: Int, value: Any) = ps.setObject(ind, UUID.fromString(value as String), Types.OTHER)

			override fun sqlToJava(rs: ResultSet, ind: Int): Any? = rs.getObject(ind, String::class.java)
		})
	}

	private fun initCharTypes() {
		val strConv = object : DefaultNullTypeConverter() {
			override fun notNullJavaToSql(value: Any): String = "'${value.toString().replace("'", "''")}'"

			override fun notNullJavaToPreparedSql(ps: PreparedStatement, ind: Int, value: Any) = ps.setString(ind, value as String)

			override fun sqlToJava(rs: ResultSet, ind: Int): Any? = rs.getString(ind)
		}

		SqlTypeConvertersRegistry.registerConverter(String::class.java, VarCharType::class.java, strConv)
		SqlTypeConvertersRegistry.registerConverter(String::class.java, CharType::class.java, strConv)
		SqlTypeConvertersRegistry.registerConverter(String::class.java, TextType::class.java, strConv)
	}

	private fun initNumberTypes() {
		SqlTypeConvertersRegistry.registerConverter(String::class.java, IntType::class.java, object: DefaultNullTypeConverter() {
			override fun notNullJavaToSql(value: Any): String = "${Integer.parseInt(value as String)}"

			override fun notNullJavaToPreparedSql(ps: PreparedStatement, ind: Int, value: Any) = ps.setInt(ind, Integer.parseInt(value as String))

			override fun sqlToJava(rs: ResultSet, ind: Int): Any? = rs.getInt(ind)
		})
		SqlTypeConvertersRegistry.registerConverter(Int::class.java, IntType::class.java, object: DefaultNullTypeConverter() {
			override fun notNullJavaToSql(value: Any): String = "$value"

			override fun notNullJavaToPreparedSql(ps: PreparedStatement, ind: Int, value: Any) = ps.setInt(ind, value as Int)

			override fun sqlToJava(rs: ResultSet, ind: Int): Any? = rs.getInt(ind)
		})
		SqlTypeConvertersRegistry.registerConverter(Integer::class.java, IntType::class.java, object: DefaultNullTypeConverter() {
			override fun notNullJavaToSql(value: Any): String = "$value"

			override fun notNullJavaToPreparedSql(ps: PreparedStatement, ind: Int, value: Any) = ps.setInt(ind, value as Int)

			override fun sqlToJava(rs: ResultSet, ind: Int): Any? = rs.getInt(ind)
		})
		SqlTypeConvertersRegistry.registerConverter(Long::class.java, BigIntType::class.java, object: DefaultNullTypeConverter() {
			override fun notNullJavaToSql(value: Any): String = "$value"

			override fun notNullJavaToPreparedSql(ps: PreparedStatement, ind: Int, value: Any) = ps.setLong(ind, value as Long)

			override fun sqlToJava(rs: ResultSet, ind: Int): Any = rs.getLong(ind)
		})
		SqlTypeConvertersRegistry.registerConverter(java.lang.Long::class.java, BigIntType::class.java, object: DefaultNullTypeConverter() {
			override fun notNullJavaToSql(value: Any): String = "$value"

			override fun notNullJavaToPreparedSql(ps: PreparedStatement, ind: Int, value: Any) = ps.setLong(ind, (value as java.lang.Long).toLong())

			override fun sqlToJava(rs: ResultSet, ind: Int): Any = rs.getLong(ind)
		})
		SqlTypeConvertersRegistry.registerConverter(Double::class.java, NumericType::class.java, object: DefaultNullTypeConverter() {
			override fun notNullJavaToSql(value: Any): String = "$value"

			override fun notNullJavaToPreparedSql(ps: PreparedStatement, ind: Int, value: Any) = ps.setDouble(ind, value as Double)

			override fun sqlToJava(rs: ResultSet, ind: Int): Any = rs.getDouble(ind)
		})
		SqlTypeConvertersRegistry.registerConverter(java.lang.Double::class.java, NumericType::class.java, object: DefaultNullTypeConverter() {
			override fun notNullJavaToSql(value: Any): String = "$value"

			override fun notNullJavaToPreparedSql(ps: PreparedStatement, ind: Int, value: Any) = ps.setDouble(ind, value as Double)

			override fun sqlToJava(rs: ResultSet, ind: Int): Any = rs.getDouble(ind)
		})
		SqlTypeConvertersRegistry.registerConverter(Instant::class.java, BigIntType::class.java, object: DefaultNullTypeConverter() {
			override fun notNullJavaToSql(value: Any): String = "${(value as Instant).toEpochMilli()}"

			override fun notNullJavaToPreparedSql(ps: PreparedStatement, ind: Int, value: Any) = ps.setLong(ind, (value as Instant).toEpochMilli())

			override fun sqlToJava(rs: ResultSet, ind: Int): Any? = Instant.ofEpochMilli(rs.getLong(ind))
		})
	}

	private fun initByteaTypes() {
		SqlTypeConvertersRegistry.registerConverter(ByteArray::class.java, ByteaType::class.java, object: DefaultNullTypeConverter() {
			override fun notNullJavaToSql(value: Any): String = "$value"

			override fun notNullJavaToPreparedSql(ps: PreparedStatement, ind: Int, value: Any) = ps.setBytes(ind, value as ByteArray)

			override fun sqlToJava(rs: ResultSet, ind: Int): Any? = rs.getBytes(ind)
		})
		SqlTypeConvertersRegistry.registerConverter(String::class.java, ByteaType::class.java, object: DefaultNullTypeConverter() {
			override fun notNullJavaToSql(value: Any): String = "'$value'"

			override fun notNullJavaToPreparedSql(ps: PreparedStatement, ind: Int, value: Any) = ps.setBytes(ind, (value as String).toByteArray())

			override fun sqlToJava(rs: ResultSet, ind: Int): Any? = rs.getBytes(ind).toString(Charsets.UTF_8)
		})
	}

	private fun initUuidArrayType() {
		SqlTypeConvertersRegistry.registerConverter(UUIDArray::class.java, UuidArrayType::class.java, object: DefaultNullTypeConverter() {
			override fun notNullJavaToSql(value: Any): String {
				val arr = value as UUIDArray
				return "'{" + arr.joinToString(",") { "$it" } + "}'"
			}

			override fun notNullJavaToPreparedSql(ps: PreparedStatement, ind: Int, value: Any) {
				val sqlArr = (value as UUIDArray).toArray()
				ps.setArray(ind, ps.connection.createArrayOf("uuid", sqlArr))
			}

			override fun sqlToJava(rs: ResultSet, ind: Int): Any? = UUIDArray().apply { addAll(rs.getArray(ind) as Array<UUID>) }
		})
	}

	private fun initBooleanType() {
		SqlTypeConvertersRegistry.registerConverter(Boolean::class.java, BooleanType::class.java, object: DefaultNullTypeConverter() {
			override fun notNullJavaToSql(value: Any): String = "$value"

			override fun notNullJavaToPreparedSql(ps: PreparedStatement, ind: Int, value: Any) = ps.setBoolean(ind, value as Boolean)

			override fun sqlToJava(rs: ResultSet, ind: Int): Any? = rs.getBoolean(ind)
		})
		SqlTypeConvertersRegistry.registerConverter(java.lang.Boolean::class.java, BooleanType::class.java, object: DefaultNullTypeConverter() {
			override fun notNullJavaToSql(value: Any): String = "$value"

			override fun notNullJavaToPreparedSql(ps: PreparedStatement, ind: Int, value: Any) = ps.setBoolean(ind, (value as java.lang.Boolean).booleanValue())

			override fun sqlToJava(rs: ResultSet, ind: Int): Any? = rs.getBoolean(ind)
		})
	}

	private fun initDateType() {
		SqlTypeConvertersRegistry.registerConverter(LocalDate::class.java, DateType::class.java, object: DateAndTimeNullTypeConverter() {
			override fun notNullJavaToSql(value: Any): String {
				if (value == LocalDate.MIN)
					return "'$negativeInfinity'"
				if (value == LocalDate.MAX)
					return "'$positiveInfinity'"
				return "'$value'"
			}

			override fun notNullJavaToPreparedSql(ps: PreparedStatement, ind: Int, value: Any) {
				if (value == LocalDate.MIN || value == LocalDate.MAX) {
					val obj = PGobject()
					obj.type = "date"
					obj.value = if (value == LocalDate.MIN) negativeInfinity else positiveInfinity
					ps.setObject(ind, obj)
				} else {
					ps.setObject(ind, value as LocalDate)
				}
			}

			val min = LocalDateTime.MIN.toLocalDate()
			val max = LocalDateTime.MAX.toLocalDate()

			override fun sqlToJava(rs: ResultSet, ind: Int): Any? {
				val d = rs.getObject(ind, LocalDate::class.java)
				if (d == min)
					return LocalDate.MIN
				if (d == max)
					return LocalDate.MAX
				return d
			}
		})
	}

	private fun initTimeType() {
		SqlTypeConvertersRegistry.registerConverter(LocalTime::class.java, TimeType::class.java, object: DefaultNullTypeConverter() {
			override fun notNullJavaToSql(value: Any): String = "'$value'"

			override fun notNullJavaToPreparedSql(ps: PreparedStatement, ind: Int, value: Any) = ps.setObject(ind, value as LocalTime)

			override fun sqlToJava(rs: ResultSet, ind: Int): Any? = rs.getObject(ind, LocalTime::class.java)
		})
	}

	private fun initTimeStampType() {
		SqlTypeConvertersRegistry.registerConverter(LocalDateTime::class.java, TimeStampType::class.java, object: DateAndTimeNullTypeConverter() {
			override fun notNullJavaToSql(value: Any): String {
				if (value == LocalDateTime.MIN)
					return "'$negativeInfinity'"
				if (value == LocalDateTime.MAX)
					return "'$positiveInfinity'"
				return "'$value'"
			}

			override fun notNullJavaToPreparedSql(ps: PreparedStatement, ind: Int, value: Any) = ps.setObject(ind, value as LocalDateTime)

			override fun sqlToJava(rs: ResultSet, ind: Int): Any? = rs.getObject(ind, LocalDateTime::class.java)
		})
	}

	private fun initTimeStampWithTimeZoneType() {
		SqlTypeConvertersRegistry.registerConverter(Instant::class.java, TimeStampWithTimeZoneType::class.java, object: DateAndTimeNullTypeConverter() {
			val tzUTC = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

			override fun notNullJavaToSql(value: Any): String {
				val instant = value as Instant
				if (instant == Instant.MIN)
					return "'$negativeInfinity'"
				if (instant == Instant.MAX)
					return "'$positiveInfinity'"
				return  "'$value'"
			}

			override fun notNullJavaToPreparedSql(ps: PreparedStatement, ind: Int, value: Any) {
				val instant = value as Instant
				if (instant == Instant.MIN)
					ps.setString(ind, negativeInfinity)
				else if (instant == Instant.MAX)
					ps.setString(ind, positiveInfinity)
				else {
					val ts = Timestamp.from(instant)
					ps.setTimestamp(ind, ts, tzUTC)
				}
			}

			override fun sqlToJava(rs: ResultSet, ind: Int): Any? {
				val asString = rs.getString(ind)
				if (asString == negativeInfinity)
					return Instant.MIN
				else if (asString == positiveInfinity)
					return Instant.MAX
				val ts = rs.getTimestamp(ind, tzUTC)
				return ts?.toInstant()
			}
		})
	}

	private fun initJsonTypes() {
		fun jsonConverter(pgType: String) = object: DefaultNullTypeConverter() {
			override fun notNullJavaToSql(value: Any): String {
				val json = (value as JsonElement).toString().replace("'", "''")
				return "'$json'"
			}

			override fun notNullJavaToPreparedSql(ps: PreparedStatement, ind: Int, value: Any) {
				val obj = PGobject()
				obj.type = pgType
				obj.value = (value as JsonElement).toString()
				ps.setObject(ind, obj)
			}

			override fun sqlToJava(rs: ResultSet, ind: Int): Any? {
				val s = rs.getString(ind) ?: return null
				return JsonParser.parseString(s)
			}
		}

		SqlTypeConvertersRegistry.registerConverter(JsonElement::class.java, JsonType::class.java, jsonConverter("json"))
		SqlTypeConvertersRegistry.registerConverter(JsonElement::class.java, JsonbType::class.java, jsonConverter("jsonb"))
	}

	override fun isBoolean(type: Class<out SqlType>) = BooleanType::class.java.isAssignableFrom(type)

	override fun getTables(dataSourceName: String) {
		val vendor = ConnectionManager.getVendor(dataSourceName)
		if (vendor != this)
			throw RuntimeException("This is not me")
		val connection = ConnectionManager.getConnection(dataSourceName)
		val meta = connection.metaData
		var rs = meta.getTables(null, null, "%", arrayOf("TABLE"))
		val tables = mutableListOf<SqlDataSourceInfo>()
		while (rs.next()) {
			tables.add(SqlDataSourceInfo(rs.getString(3)))
		}
		rs = meta.getColumns(null, null, null, null)
		while (rs.next()) {
			val tbl = tables.firstOrNull { it.name.equals(rs.getString(3)) }
			tbl?.columns?.add(ColumnInfo(rs.getString(4), rs.getString(6)))
		}


		tables.forEach {
			println(it.name)
			it.columns.forEach { c ->
				println("\t${c.name} ${c.type}")
			}
		}
	}

	override fun writeSqlDataSource(dataSource: SqlDataSource, builder: StringBuilder) {
		builder.append("\"")
		builder.append(dataSource.sqlName)
		builder.append("\"")
	}

	override fun writeSqlColumnName(column: TableColumn<*, *>, builder: StringBuilder) {
		builder.append("\"");
		builder.append(column.name);
		builder.append("\"");
	}

	override fun createLogicalOperation(op: LogicalOperation.Operator, values: Collection<SqlValue<*, *>>): LogicalOperation<*, *> {
		return PostgresqlLogical(op, values)
	}

	override fun createComparisonOperation(left: SqlValue<*, *>, op: ComparisonOperation.Operator, right: SqlValue<*, *>): ComparisonOperation<*, *> {
		return PostgresqlComparison(left, op, right)
	}

	override fun getCountExpression(): Expression<Long, *> {
		return CountExpression()
	}
}