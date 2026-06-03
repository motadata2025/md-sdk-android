/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.motadata.android.trace.integration.api

import com.motadata.android.trace.api.span.MotadataSpan
import com.motadata.android.trace.internal._TraceInternalProxy

/**
 * Returns the span's least significant trace id in hex format (the last 64 bits from the 128 bits trace id)
 */
fun MotadataSpan.leastSignificant64BitsTraceId(): String {
    return traceId.toHexString().padStart(32, '0').takeLast(16)
}

/**
 * Returns the span's most significant trace id in hex format (the first 64 bits from the 128 bits trace id)
 */
fun MotadataSpan.mostSignificant64BitsTraceId(): String {
    return traceId.toHexString().padStart(32, '0').take(16)
}

/**
 * Returns the span's spanId in hex format.
 * The [MotadataSpanContext.toSpanId] method returns a string in decimal format,
 * which doesn't match what we send in our events
 */
fun MotadataSpan.spanIdAsHexString(): String {
    return _TraceInternalProxy.spanIdConverter.toHexStringPadded(context().spanId)
}
