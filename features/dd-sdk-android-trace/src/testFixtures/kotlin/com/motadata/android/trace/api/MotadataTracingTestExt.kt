/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.motadata.android.trace.api

import com.motadata.android.trace.GlobalDatadogTracer
import com.motadata.android.trace.api.span.MotadataSpan
import com.motadata.android.trace.api.span.MotadataSpanContext
import com.motadata.android.trace.api.trace.MotadataTraceId
import com.motadata.android.trace.api.tracer.MotadataTracer
import com.motadata.android.trace.api.tracer.MotadataTracerBuilder
import com.motadata.android.trace.internal.MotadataPropagationHelper
import com.motadata.android.trace.internal.MotadataSpanAdapter
import com.motadata.android.trace.internal.MotadataSpanContextAdapter
import com.motadata.android.trace.internal.MotadataTraceIdAdapter
import com.motadata.android.trace.internal.MotadataTracerAdapter
import com.motadata.android.trace.internal.MotadataTracerBuilderAdapter
import com.motadata.android.trace.internal._TraceInternalProxy
import com.datadog.trace.api.DDTraceId
import com.datadog.trace.core.CoreTracer
import com.datadog.trace.core.DDSpanContext

val MotadataTracer.partialFlushMinSpans: Int?
    get() = coreTracer?.partialFlushMinSpans

val MotadataSpanContext.resourceName: String?
    get() = ddSpanContext?.resourceName?.toString()

val MotadataSpanContext.serviceName: String?
    get() = ddSpanContext?.serviceName?.toString()

val MotadataTraceId.Companion.ZERO: MotadataTraceId
    get() = MotadataTraceIdAdapter(DDTraceId.ZERO)

fun MotadataTraceId.Companion.from(traceId: Long): MotadataTraceId {
    return MotadataTraceIdAdapter(DDTraceId.from(traceId))
}

fun MotadataTraceId.Companion.from(traceId: String): MotadataTraceId {
    return MotadataTraceIdAdapter(DDTraceId.from(traceId))
}

fun MotadataSpan.forceSamplingDecision() {
    (this as MotadataSpanAdapter).delegate.forceSamplingDecision()
}

fun _TraceInternalProxy.setTracingAdapterBuilderMock(mock: MotadataTracerBuilder?) {
    testBuilderProvider = mock
}

fun _TraceInternalProxy.clear() {
    setTracingAdapterBuilderMock(null)
}

fun _TraceInternalProxy.withMockPropagationHelper(
    mockHelper: MotadataPropagationHelper,
    block: _TraceInternalProxy.() -> Unit
) {
    val helper = propagationHelper
    try {
        propagationHelper = mockHelper
        block()
    } finally {
        propagationHelper = helper
    }
}

fun MotadataTracerBuilder.setTestIdGenerationStrategy(strategy: TestIdGenerationStrategy) = apply {
    (this as? MotadataTracerBuilderAdapter)?.setCustomIdGenerationStrategy(strategy)
}

fun GlobalDatadogTracer.replace(
    builder: MotadataTracerBuilder
): Boolean {
    clear()
    return registerIfAbsent(builder.build())
}

private val MotadataSpanContext.ddSpanContext: DDSpanContext?
    get() {
        val spanContextAdapter = this as? MotadataSpanContextAdapter
        return spanContextAdapter?.delegate as? DDSpanContext
    }

private val MotadataTracer.coreTracer: CoreTracer?
    get() {
        val tracerAdapter = this as? MotadataTracerAdapter
        return tracerAdapter?.delegate as? CoreTracer
    }
