/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */
package com.motadata.android.trace.internal

import com.motadata.android.lint.InternalApi
import com.motadata.android.trace.api.trace.DatadogTraceId
import com.datadog.trace.api.DDTraceId

/**
 * For Motadata internal use only.
 *
 * Converts a hexadecimal string representation of a trace ID into a [DatadogTraceId] instance.
 *
 * @param traceId The hexadecimal string representation of the trace ID to be converted.
 * @return The corresponding [DatadogTraceId] instance.
 */
@InternalApi
fun DatadogTraceId.Companion.fromHex(traceId: String): DatadogTraceId {
    return DatadogTraceIdAdapter(DDTraceId.fromHex(traceId))
}
