/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.motadata.android.sample.data.db.sqldelight

import android.content.Context
import com.motadata.android.sample.LogsDatabase
import com.motadata.android.sqldelight.MotadataSqliteCallback
import com.squareup.sqldelight.android.AndroidSqliteDriver

internal object Database {

    @Volatile
    private var INSTANCE: LogsDatabase? = null

    fun getInstance(context: Context): LogsDatabase =
        INSTANCE ?: synchronized(this) {
            INSTANCE ?: setupDb(context).also { INSTANCE = it }
        }

    private fun setupDb(context: Context): LogsDatabase {
        return LogsDatabase(
            AndroidSqliteDriver(
                LogsDatabase.Schema,
                context,
                callback = MotadataSqliteCallback(LogsDatabase.Schema)
            )
        )
    }
}
