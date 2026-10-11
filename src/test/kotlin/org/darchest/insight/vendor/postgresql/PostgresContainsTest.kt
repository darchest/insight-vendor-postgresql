package org.darchest.insight.vendor.postgresql

import kotlinx.coroutines.runBlocking
import org.darchest.insight.SqlConst
import kotlin.test.Test
import kotlin.test.assertEquals

class PostgresContainsTest {

    class TagTable : PostgresTable("tags") {
        val name by VarCharCol("name")
        val ids by UUIDArrayCol("ids")
    }

    @Test
    fun sql_contains() = runBlocking {
        val tbl = TagTable()
        val right = SqlConst(UUIDArray(), UUIDArray::class.java, UuidArrayType())
        val (sql, params) = (tbl.ids contains right).getSql(PostgresVendor)

        assertEquals(""""ids" @> ?""", sql)
        assertEquals(1, params.size)
        assertEquals(right, params[0])
    }
}
