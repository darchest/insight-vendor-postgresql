package org.darchest.insight.vendor.postgresql

import org.darchest.insight.SqlValue
import org.darchest.insight.Vendor

class PostgresqlPlus<javaType: Any, sqlT: PostgresqlType>(
    private val columns: Collection<SqlValue<javaType, sqlT>>,
    javaClass: Class<javaType>,
    sqlType: sqlT
): SqlValue<javaType, sqlT>(javaClass, sqlType) {

    override suspend fun writeSql(builder: StringBuilder, vendor: Vendor, params: MutableList<SqlValue<*, *>>) {
        builder.append("(")
        if (columns.isEmpty())
            builder.append("0")
        else {
            columns.first().writeSql(builder, vendor, params)
            for (c in columns.drop(1)) {
                builder.append(" + ")
                c.writeSql(builder, vendor, params)
            }
        }
        builder.append(")")
    }

    override fun fillByInnerColumns(array: MutableCollection<SqlValue<*, *>>) {
        columns.forEach { it.innerColumns(array) }
    }
}