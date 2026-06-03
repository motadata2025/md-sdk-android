/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.motadata.android.trace.opentelemetry.internal

import android.os.Build
import androidx.annotation.RequiresApi
import io.opentelemetry.context.ContextStorage
import java.util.function.Function

@RequiresApi(Build.VERSION_CODES.N)
internal class MotadataContextStorageWrapper : Function<ContextStorage, MotadataContextStorage> {
    override fun apply(wrapped: ContextStorage): MotadataContextStorage {
        return if (wrapped is MotadataContextStorage) wrapped else MotadataContextStorage(wrapped)
    }
}
