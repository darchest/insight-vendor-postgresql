/*
 * Copyright 2021-2024, Darchest and contributors.
 * Licensed under the Apache License, Version 2.0
 */

package org.darchest.insight.vendor.postgresql

import org.darchest.insight.SqlValue
import org.darchest.insight.Vendor

class PostgresArrayContains(val left: SqlValue<*, *>, val right: SqlValue<*, *>): SqlValue<Boolean, BooleanType>(Boolean::class.java, BooleanType()) {

    override suspend fun writeSql(builder: StringBuilder, vendor: Vendor, params: MutableList<SqlValue<*, *>>) {
        left.writeSql(builder, vendor, params)
        builder.append(" && ")
        right.writeSql(builder, vendor, params)
    }

    override fun fillByInnerColumns(array: MutableCollection<SqlValue<*, *>>) {
        left.innerColumns(array)
        right.innerColumns(array)
    }
}