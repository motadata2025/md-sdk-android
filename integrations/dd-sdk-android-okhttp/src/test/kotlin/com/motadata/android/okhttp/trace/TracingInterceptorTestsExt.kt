/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */
package com.motadata.android.okhttp.trace

import com.motadata.android.core.sampling.Sampler
import com.motadata.android.trace.api.propagation.MotadataPropagation
import com.motadata.android.trace.api.span.MotadataSpan
import com.motadata.android.trace.api.span.MotadataSpanBuilder
import com.motadata.android.trace.api.span.MotadataSpanContext
import com.motadata.android.trace.api.trace.MotadataTraceId
import com.motadata.android.trace.api.tracer.MotadataTracer
import com.motadata.android.trace.internal.fromHex
import fr.xgouchet.elmyr.Forge
import okhttp3.Request
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

internal fun newAgentPropagationMock(
    extractedContext: MotadataSpanContext = mock()
) = mock<MotadataPropagation> {
    on { extract(any<Request>(), any()) } doReturn extractedContext
}
internal fun MotadataPropagation.wheneverInjectThenThrow(throwable: Throwable) {
    doThrow(throwable)
        .whenever(this)
        .inject(any<MotadataSpanContext>(), any<Request.Builder>(), any())
}

internal fun MotadataPropagation.wheneverInjectThenValueToHeaders(key: String, value: String) {
    doAnswer { invocation ->
        val carrier = invocation.getArgument<Request.Builder>(1)
        val setter = invocation.getArgument<(carrier: Request.Builder, key: String, value: String) -> Unit>(2)
        setter.invoke(carrier, key, value)
    }
        .whenever(this)
        .inject(any<MotadataSpanContext>(), any<Request.Builder>(), any())
}

internal fun MotadataPropagation.wheneverInjectCalledPassContextToHeaders(
    datadogContext: Map<String, String>,
    nonDatadogContextKey: String,
    nonDatadogContextKeyValue: String
) {
    doAnswer { invocation ->
        val carrier = invocation.getArgument<Request.Builder>(1)
        val setter = invocation.getArgument<(carrier: Request.Builder, key: String, value: String) -> Unit>(2)
        datadogContext.forEach { setter.invoke(carrier, it.key, it.value) }
        setter.invoke(carrier, nonDatadogContextKey, nonDatadogContextKeyValue)
    }
        .whenever(this)
        .inject(any<MotadataSpanContext>(), any<Request.Builder>(), any())
}

internal fun Forge.aDatadogTraceId(
    fakeString: String? = null
) = MotadataTraceId.fromHex(fakeString ?: aStringMatching("[a-f0-9]{31}"))

internal fun Forge.newTraceSamplerMock(
    span: MotadataSpan = newSpanMock()
) = mock<Sampler<MotadataSpan>> {
    on { sample(span) } doReturn true
}

internal fun Forge.newTracerMock(
    spanBuilder: MotadataSpanBuilder = newSpanBuilderMock(),
    propagation: MotadataPropagation = newAgentPropagationMock()
) = mock<MotadataTracer> {
    on { buildSpan(TracingInterceptor.SPAN_NAME) } doReturn spanBuilder
    on { propagate() } doReturn propagation
}

internal inline fun <reified T : MotadataSpanContext> Forge.newSpanContextMock(
    fakeTraceId: MotadataTraceId = aDatadogTraceId(),
    fakeSpanId: Long = aLong(),
    samplingPriority: Int = 0
): T = mock<T> {
    on { spanId } doReturn fakeSpanId
    on { traceId } doReturn fakeTraceId
    on { mock.samplingPriority } doReturn samplingPriority
}

internal fun Forge.newSpanMock(
    context: MotadataSpanContext = newSpanContextMock(),
    samplingPriority: Int? = null
) = mock<MotadataSpan> {
    on { context() } doReturn context
    on { this.samplingPriority } doReturn samplingPriority
}

internal fun Forge.newSpanBuilderMock(
    localSpan: MotadataSpan = newSpanMock(),
    context: MotadataSpanContext = newSpanContextMock()
) = mock<MotadataSpanBuilder> {
    on { withOrigin(anyOrNull()) } doReturn it
    on { withParentContext(context) } doReturn it
    on { withParentContext(null as MotadataSpanContext?) } doReturn it
    on { start() } doReturn localSpan
}
