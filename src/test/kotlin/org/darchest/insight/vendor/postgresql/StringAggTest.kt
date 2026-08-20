package org.darchest.insight.vendor.postgresql

import kotlinx.coroutines.runBlocking
import org.darchest.insight.SqlConst
import org.darchest.insight.impl.select
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StringAggTest {

    class CommentTable : PostgresTable("comments") {
        val id by UUIDCol("id")
        val userId by UUIDCol("user_id")
        val body by VarCharCol("body")
        val active by BoolCol("active")
    }

    class UserTable : PostgresTable("users") {
        val id by UUIDCol("id")
        val name by VarCharCol("name")
        val age by IntCol("age")
        val comments by JoinDelegate(::CommentTable, { t -> t.userId eq id })
    }

    @Test
    fun sql_basic() = runBlocking {
        val tbl = UserTable()
        val (sql, params) = StringAgg(tbl.name, ",").getSql(PostgresVendor)

        assertEquals("""string_agg("name", ?)""", sql)
        assertEquals(1, params.size)
        assertEquals(",", (params[0] as SqlConst<*, *>).valuee)
    }

    @Test
    fun sql_distinct() = runBlocking {
        val tbl = UserTable()
        val (sql, _) = StringAgg(tbl.name, ",", distinct = true).getSql(PostgresVendor)

        assertEquals("""string_agg(DISTINCT "name", ?)""", sql)
    }

    @Test
    fun sql_string_field_no_cast() = runBlocking {
        val tbl = UserTable()
        val (sql, _) = stringAgg(tbl.name, ";").getSql(PostgresVendor)

        assertEquals("""string_agg("name", ?)""", sql)
        assertTrue(!sql.contains("::text"))
    }

    @Test
    fun sql_non_string_field_casts_to_text() = runBlocking {
        val tbl = UserTable()
        val (sql, _) = StringAgg(tbl.id, ",").getSql(PostgresVendor)

        assertEquals("""string_agg(("id")::text, ?)""", sql)
    }

    @Test
    fun sql_filter() = runBlocking {
        val tbl = UserTable()
        val (sql, params) = StringAgg(
            tbl.name,
            ",",
            filter = tbl.age eq 18
        ).getSql(PostgresVendor)

        assertEquals("""string_agg("name", ?) FILTER (WHERE "age" = ?)""", sql)
        assertEquals(2, params.size)
        assertEquals(",", (params[0] as SqlConst<*, *>).valuee)
        assertEquals(18, (params[1] as SqlConst<*, *>).valuee)
    }

    @Test
    fun sql_order_by_asc() = runBlocking {
        val tbl = UserTable()
        val (sql, _) = StringAgg(
            tbl.name,
            ",",
            orderBy = listOf(tbl.name.asc())
        ).getSql(PostgresVendor)

        assertEquals("""string_agg("name", ? ORDER BY "name" ASC)""", sql)
    }

    @Test
    fun sql_order_by_multi() = runBlocking {
        val tbl = UserTable()
        val (sql, _) = StringAgg(
            tbl.name,
            ",",
            orderBy = listOf(tbl.age.desc(), tbl.name.asc())
        ).getSql(PostgresVendor)

        assertEquals("""string_agg("name", ? ORDER BY "age" DESC, "name" ASC)""", sql)
    }

    @Test
    fun sql_distinct_order_by_filter() = runBlocking {
        val tbl = UserTable()
        val (sql, params) = StringAgg(
            tbl.name,
            "|",
            distinct = true,
            filter = tbl.age gt 0,
            orderBy = listOf(tbl.name.asc())
        ).getSql(PostgresVendor)

        assertEquals(
            """string_agg(DISTINCT "name", ? ORDER BY "name" ASC) FILTER (WHERE "age" > ?)""",
            sql
        )
        assertEquals(2, params.size)
        assertEquals("|", (params[0] as SqlConst<*, *>).valuee)
        assertEquals(0, (params[1] as SqlConst<*, *>).valuee)
    }

    @Test
    fun sql_via_select() = runBlocking {
        val tbl = UserTable()

        val cursor = select(tbl) {
            fields(StringAgg(tbl.name, ","))
        }

        val (sql, _) = cursor.getSql(PostgresVendor)
        assertEquals(
            """
            |SELECT string_agg("name", ?)
            |FROM "users"
            """.trimMargin(),
            sql
        )
    }

    @Test
    fun sql_join_column_inside_string_agg() = runBlocking {
        val tbl = UserTable()

        val cursor = select(tbl) {
            fields(StringAgg(tbl.comments().body, ",", orderBy = listOf(tbl.comments().body.asc())))
        }

        val (sql, _) = cursor.getSql(PostgresVendor)
        assertEquals(
            """
            |SELECT string_agg(T1."body", ? ORDER BY T1."body" ASC)
            |FROM "users" T0
	        |	INNER JOIN "comments" T1 ON T1."user_id" = T0."id"
            """.trimMargin(),
            sql
        )
    }

    @Test
    fun sql_string_agg_with_group_by() = runBlocking {
        val tbl = UserTable()

        val cursor = select(tbl) {
            groupBy(tbl.id)
            fields(StringAgg(tbl.comments().body, ",", orderBy = listOf(tbl.comments().body.asc())))
        }

        val (sql, _) = cursor.getSql(PostgresVendor)
        assertEquals(
            """
            |SELECT T0."id", string_agg(T1."body", ? ORDER BY T1."body" ASC)
            |FROM "users" T0
	        |	INNER JOIN "comments" T1 ON T1."user_id" = T0."id"
            |GROUP BY T0."id"
            """.trimMargin(),
            sql
        )
    }
}
