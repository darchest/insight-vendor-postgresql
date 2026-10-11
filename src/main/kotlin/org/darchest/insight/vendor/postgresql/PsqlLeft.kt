package org.darchest.insight.vendor.postgresql

import org.darchest.insight.SqlDataSource
import org.darchest.insight.SqlValue
import org.darchest.insight.Vendor

class PsqlLeft(
    val text: SqlValue<*, StringType>,
    val length: Int,
) : SqlValue<String, StringType>(String::class.java, VarCharType()) {

    override suspend fun writeSql(builder: StringBuilder, vendor: Vendor, params: MutableList<SqlValue<*, *>>) {
        builder.append("LEFT(")
        text.writeSql(builder, vendor, params)
        builder.append(", ")
        builder.append(length)
        builder.append(")")
    }

    override fun fillByInnerColumns(array: MutableCollection<SqlValue<*, *>>) {
        text.innerColumns(array)
    }

    override fun collectReferencedSources(out: MutableSet<SqlDataSource>) {
        text.collectReferencedSources(out)
    }
}
