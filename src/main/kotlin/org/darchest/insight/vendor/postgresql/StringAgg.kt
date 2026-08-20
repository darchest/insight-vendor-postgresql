/*
 * Copyright 2021-2024, Darchest and contributors.
 * Licensed under the Apache License, Version 2.0
 */

package org.darchest.insight.vendor.postgresql

import org.darchest.insight.SortInfo
import org.darchest.insight.SqlConst
import org.darchest.insight.SqlValue
import org.darchest.insight.Vendor

class StringAgg(
    val field: SqlValue<*, *>,
    val delimiter: String,
    val distinct: Boolean = false,
    val filter: SqlValue<*, BooleanType>? = null,
    val orderBy: List<SortInfo> = emptyList(),
): PostgresqlExpression<String, TextType>(String::class.java, TextType()) {
    override suspend fun writeSql(
        builder: StringBuilder,
        vendor: Vendor,
        params: MutableList<SqlValue<*, *>>
    ) {
        builder.append("string_agg(")
        if (distinct)
            builder.append("DISTINCT ")
        val needsTextCast = field.sqlType !is StringType
        if (needsTextCast)
            builder.append("(")
        field.writeSql(builder, vendor, params)
        if (needsTextCast)
            builder.append(")::text")
        builder.append(", ")
        SqlConst(delimiter, String::class.java, TextType())
            .writeSql(builder, vendor, params)
        if (orderBy.isNotEmpty()) {
            builder.append(" ORDER BY ")
            val iter = orderBy.iterator()
            iter.next().writeSql(builder, vendor, params)
            while (iter.hasNext()) {
                builder.append(", ")
                iter.next().writeSql(builder, vendor, params)
            }
        }
        builder.append(")")
        if (filter != null) {
            builder.append(" FILTER (WHERE ")
            filter.writeSql(builder, vendor, params)
            builder.append(")")
        }
    }

    override fun fillByInnerColumns(array: MutableCollection<SqlValue<*, *>>) {
        super.fillByInnerColumns(array)
        field.innerColumns(array)
        filter?.innerColumns(array)
        orderBy.forEach { it.expr.innerColumns(array) }
    }
}
