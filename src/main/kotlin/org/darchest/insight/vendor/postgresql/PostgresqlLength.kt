package org.darchest.insight.vendor.postgresql

import org.darchest.insight.SqlDataSource
import org.darchest.insight.SqlValue
import org.darchest.insight.Vendor

class PostgresqlLength(val text: SqlValue<*, StringType>): SqlValue<Int, IntType>(Int::class.java, IntType()) {
    override suspend fun writeSql(builder: StringBuilder, vendor: Vendor, params: MutableList<SqlValue<*, *>>) {
        builder.append("length(")
        text.writeSql(builder, vendor, params)
        builder.append(")")
    }

    override fun fillByInnerColumns(array: MutableCollection<SqlValue<*, *>>) {
        text.innerColumns(array)
    }

    override fun collectReferencedSources(out: MutableSet<SqlDataSource>) {
        text.collectReferencedSources(out)
    }
}