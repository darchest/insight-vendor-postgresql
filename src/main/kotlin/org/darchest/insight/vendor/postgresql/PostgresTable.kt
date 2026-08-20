/*
 * Copyright 2021-2024, Darchest and contributors.
 * Licensed under the Apache License, Version 2.0
 */

package org.darchest.insight.vendor.postgresql

import com.google.gson.JsonElement
import com.google.gson.JsonNull
import org.darchest.insight.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*
import kotlin.reflect.KProperty

open class PostgresTable(name: String): Table(name) {

	override fun vendor(): Vendor = PostgresVendor

	open class JoinDelegate<T: PostgresTable>(private val tableFactory: () -> T, private val where: (T) -> SqlValue<*, *>, private val type: Join.Type = Join.Type.INNER) {
		lateinit var join: Join<T>

		operator fun provideDelegate(thisRef: PostgresTable, prop: KProperty<*>): JoinDelegate<T> {
			join = Join(thisRef, tableFactory)
			join.codeName = (if (thisRef.joined != null) thisRef.joined!!.codeName + prop.name else prop.name) + "."
			thisRef.joinByNames[prop.name] = join
			return this
		}

		operator fun getValue(thisRef: PostgresTable, prop: KProperty<*>): Join<T> {
			if (!join.inited) {
				join.codeName = (if (thisRef.joined != null) thisRef.joined!!.codeName + prop.name else prop.name) + "."
				val t = join()
				join.expr { where.invoke(t) }
				join.type(type)
			}
			return join
		}
	}

	open class ColDelegate<T: PostgresqlColumn<*, *>>(private val col: T) {

		operator fun provideDelegate(thisRef: PostgresTable, prop: KProperty<*>): ColDelegate<T> {
			thisRef.registerColumn(col)
			col.sqlDataSource = thisRef
			col.codeName = if (thisRef.joined != null) thisRef.joined!!.codeName + prop.name else prop.name
			thisRef.sqlByNames[prop.name] = col
			thisRef.namesBySql[col] = prop.name
			return this
		}

		operator fun getValue(thisRef: PostgresTable, property: KProperty<*>): T {
			return col
		}
	}

	class UUIDCol(
		name: String,
		defaultValue: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
	): ColDelegate<UuidColumn>(UuidColumn(name, defaultValue))

	class UUIDArrayCol(name: String, defaultValue: UUIDArray = UUIDArray()): ColDelegate<UuidArrayColumn>(UuidArrayColumn(name, defaultValue))


	class VarCharCol(name: String, length: Int? = null, defaultValue: String = ""): ColDelegate<VarCharColumn>(VarCharColumn(name, length, defaultValue))

	class ByteaTextCol(name: String, defaultValue: String = ""): ColDelegate<ByteaTextColumn>(ByteaTextColumn(name, defaultValue))

	class BinaryCol(name: String, defaultValue: ByteArray = byteArrayOf()): ColDelegate<BinaryColumn>(BinaryColumn(name, defaultValue))

	class NumericCol(name: String, defaultValue: Double = 0.0): ColDelegate<NumericColumn>(NumericColumn(name, defaultValue))

	class ShortCol(name: String, defaultValue: Short = 0): ColDelegate<ShortColumn>(ShortColumn(name, defaultValue))

	class IntCol(name: String, defaultValue: Int = 0): ColDelegate<IntColumn>(IntColumn(name, defaultValue))

	class LongCol(name: String, defaultValue: Long = 0L): ColDelegate<LongColumn>(LongColumn(name, defaultValue))

	class BoolCol(name: String, defaultValue: Boolean = false): ColDelegate<BoolColumn>(BoolColumn(name, defaultValue))

	class LocalDateCol(
		name: String,
		defaultValue: LocalDate = LocalDate.MIN
	): ColDelegate<LocalDateColumn>(LocalDateColumn(name, defaultValue))

	class LocalTimeCol(
		name: String,
		defaultValue: LocalTime = LocalTime.MIN
	): ColDelegate<LocalTimeColumn>(LocalTimeColumn(name, defaultValue))

	class LocalDateTimeCol(
		name: String,
		defaultValue: LocalDateTime = LocalDateTime.MIN
	): ColDelegate<LocalDateTimeColumn>(LocalDateTimeColumn(name, defaultValue))

	class InstantCol(name: String, defaultValue: Instant = Instant.MIN): ColDelegate<InstantColumn>(InstantColumn(name, defaultValue))

	class JsonCol(
		name: String,
		defaultValue: JsonElement = JsonNull.INSTANCE
	): ColDelegate<JsonColumn>(JsonColumn(name, defaultValue))

	class JsonbCol(
		name: String,
		defaultValue: JsonElement = JsonNull.INSTANCE
	): ColDelegate<JsonbColumn>(JsonbColumn(name, defaultValue))


	fun <T: PostgresTable> countExpr() = CountExpression()

	open class ExprDelegate<T: PostgresqlExpression<*, *>>(private val expr: T) {

		operator fun provideDelegate(thisRef: PostgresTable, prop: KProperty<*>): ExprDelegate<T> {
			expr.sqlDataSource = thisRef
			expr.codeName = if (thisRef.joined != null) thisRef.joined!!.codeName + prop.name else prop.name
			thisRef.sqlByNames[prop.name] = expr
			thisRef.namesBySql[expr] = prop.name
			return this
		}

		operator fun getValue(thisRef: PostgresTable, property: KProperty<*>): T {
			return expr
		}
	}

	class StringExpr(value: SqlValue<*, VarCharType>): ExprDelegate<StringExpression>(StringExpression(value))

	class BoolExpr(exprFn: () -> SqlValue<Boolean, BooleanType>): ExprDelegate<BooleanExpression>(BooleanExpression(exprFn))

	class NumericExpr(exprFn: () -> SqlValue<Double, NumericType>): ExprDelegate<NumericExpression>(NumericExpression(exprFn))

	class StringLocalExpression(innerColumns: List<TableColumn<*, *>>, fn: suspend () -> String?): PostgresqlLocalExpression<String>(String::class.java, innerColumns, fn)

	class BooleanLocalExpression(innerColumns: List<TableColumn<*, *>>, fn: suspend () -> Boolean?): PostgresqlLocalExpression<Boolean>(Boolean::class.java, innerColumns, fn)

	class DateLocalExpression(innerColumns: List<TableColumn<*, *>>, fn: suspend () -> Instant?): PostgresqlLocalExpression<Instant>(Instant::class.java, innerColumns, fn)


	class UuidLocalExpression(innerColumns: List<TableColumn<*, *>>, fn: suspend () -> UUID?): PostgresqlLocalExpression<UUID>(UUID::class.java, innerColumns, fn)

	open class LocalExprDelegate<T: PostgresqlLocalExpression<*>>(private val expr: T) {

		operator fun provideDelegate(thisRef: PostgresTable, prop: KProperty<*>): LocalExprDelegate<T> {
			expr.sqlDataSource = thisRef
			expr.codeName = if (thisRef.joined != null) thisRef.joined!!.codeName + prop.name else prop.name
			thisRef.sqlByNames[prop.name] = expr
			thisRef.namesBySql[expr] = prop.name
			return this
		}

		operator fun getValue(thisRef: PostgresTable, property: KProperty<*>): T {
			return expr
		}
	}

	class StringLocalExpr(innerColumns: List<TableColumn<*, *>>, fn: suspend () -> String?): LocalExprDelegate<StringLocalExpression>(StringLocalExpression(innerColumns, fn))

	class BoolLocalExpr(innerColumns: List<TableColumn<*, *>>, fn: suspend () -> Boolean?): LocalExprDelegate<BooleanLocalExpression>(BooleanLocalExpression(innerColumns, fn))

	class DateLocalExpr(innerColumns: List<TableColumn<*, *>>, fn: suspend () -> Instant?): LocalExprDelegate<DateLocalExpression>(DateLocalExpression(innerColumns, fn))


	class UuidLocalExpr(innerColumns: List<TableColumn<*, *>>, fn: suspend () -> UUID?): LocalExprDelegate<UuidLocalExpression>(UuidLocalExpression(innerColumns, fn))
}