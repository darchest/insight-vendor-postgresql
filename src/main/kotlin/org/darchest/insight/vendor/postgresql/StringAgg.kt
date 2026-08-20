package org.darchest.insight.vendor.postgresql

import org.darchest.insight.Expression
import org.darchest.insight.SqlConst
import org.darchest.insight.SqlValue
import org.darchest.insight.Vendor

class StringAgg(
    val field: SqlValue<*, *>,
    val delimiter: String,
    val distinct: Boolean = false,
    val filter: SqlValue<*, BooleanType>? = null
): Expression<String, TextType>(String::class.java, TextType()) {
    override suspend fun writeSql(
        builder: StringBuilder,
        vendor: Vendor,
        params: MutableList<SqlValue<*, *>>
    ) {
        builder.append("string_agg(")
        if (distinct)
            builder.append("DISTINCT ")
        field.writeSql(builder, vendor, params)
        if (field.sqlType !is StringType)
            builder.append("::text")
        builder.append(", ")
        SqlConst(delimiter, String::class.java, TextType())
            .writeSql(builder, vendor, params)
        builder.append(")")
        if (filter != null) {
            builder.append(" FILTER (WHERE ")
            filter.writeSql(builder, vendor, params)
            builder.append(")")
        }
    }
}