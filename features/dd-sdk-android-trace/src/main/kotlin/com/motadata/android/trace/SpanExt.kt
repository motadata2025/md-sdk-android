/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.motadata.android.trace

import com.motadata.android.trace.api.span.MotadataSpan

/**
 * Wraps the provided lambda within a [MotadataSpan].
 * @param T the type returned by the lambda
 * @param operationName the name of the [MotadataSpan] created around the lambda
 * @param parentSpan the parent [MotadataSpan] (default is `null`)
 * @param activate whether the created [MotadataSpan] should be made active for the current thread
 * (default is `true`)
 * @param block the lambda function traced by this newly created [MotadataSpan]
 *
 */
@SuppressWarnings("TooGenericExceptionCaught")
inline fun <T : Any?> withinSpan(
    operationName: String,
    parentSpan: MotadataSpan? = null,
    activate: Boolean = true,
    block: MotadataSpan.() -> T
): T {
    val tracer = GlobalMotadataTracer.get()

    val span = tracer.buildSpan(operationName)
        .withParentSpan(parentSpan)
        .start()

    val scope = if (activate) tracer.activateSpan(span) else null

    return try {
        span.block()
    } catch (e: Throwable) {
        span.logThrowable(e)
        throw e
    } finally {
        span.finish()
        scope?.close()
    }
}
