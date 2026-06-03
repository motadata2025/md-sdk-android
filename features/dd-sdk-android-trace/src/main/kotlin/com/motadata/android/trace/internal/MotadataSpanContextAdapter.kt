/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */
package com.motadata.android.trace.internal

import com.motadata.android.trace.api.span.MotadataSpanContext
import com.motadata.android.trace.api.trace.MotadataTraceId
import com.datadog.trace.api.sampling.SamplingMechanism
import com.datadog.trace.bootstrap.instrumentation.api.AgentSpan
import com.datadog.trace.core.DDSpanContext
import com.datadog.trace.core.PendingTrace

internal class MotadataSpanContextAdapter(internal val delegate: AgentSpan.Context) : MotadataSpanContext {
    override val spanId: Long get() = delegate.spanId
    override val samplingPriority: Int get() = delegate.traceSamplingPriority
    override val tags: Map<String, Any?> get() = ddSpanContext?.tags.orEmpty()
    override val traceId: MotadataTraceId get() = MotadataTraceIdAdapter(delegate.traceId)

    private val ddSpanContext: DDSpanContext?
        get() = delegate as? DDSpanContext

    override fun setSamplingPriority(samplingPriority: Int): Boolean {
        return ddSpanContext?.setSamplingPriority(samplingPriority, SamplingMechanism.DEFAULT.toInt()) ?: false
    }

    override fun setMetric(key: CharSequence?, value: Double) {
        ddSpanContext?.setMetric(key, value)
    }

    internal fun setTracingSamplingPriorityIfNecessary() {
        (delegate.trace as? PendingTrace)?.setSamplingPriorityIfNecessary()
    }
}
