/*
 * Copyright 2021-2024, Darchest and contributors.
 * Licensed under the Apache License, Version 2.0
 */

package org.darchest.insight.vendor.postgresql

import org.darchest.insight.SqlDataSource
import org.darchest.insight.SqlValue
import org.darchest.insight.Vendor

class PostgresConcat(val values: List<SqlValue<*, StringType>>): SqlValue<String, StringType>(String::class.java, VarCharType()) {
    override suspend fun writeSql(builder: StringBuilder, vendor: Vendor, params: MutableList<SqlValue<*, *>>) {
        builder.append("CONCAT(")
        val iter = values.iterator()
        var value = iter.next()
        value.writeSql(builder, vendor, params)
        while (iter.hasNext()) {
            builder.append(", ")
            value = iter.next()
            value.writeSql(builder, vendor, params)
        }
        builder.append(")")
    }

    override fun fillByInnerColumns(array: MutableCollection<SqlValue<*, *>>) {
        values.forEach { it.innerColumns(array) }
    }

    override fun collectReferencedSources(out: MutableSet<SqlDataSource>) {
        values.forEach { it.collectReferencedSources(out) }
    }
}