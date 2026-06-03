/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */
package com.motadata.android.trace.internal

import com.motadata.android.api.feature.FeatureSdkCore
import com.motadata.android.trace.api.propagation.MotadataPropagation
import com.motadata.android.trace.api.scope.MotadataScope
import com.motadata.android.trace.api.scope.MotadataScopeListener
import com.motadata.android.trace.api.span.MotadataSpan
import com.motadata.android.trace.api.span.MotadataSpanBuilder
import com.motadata.android.trace.api.tracer.MotadataTracer
import com.motadata.android.trace.internal.RumContextPropagator.Companion.injectRumContext
import com.datadog.trace.bootstrap.instrumentation.api.AgentTracer
import com.datadog.trace.bootstrap.instrumentation.api.ScopeSource

internal class MotadataTracerAdapter(
    internal val sdkCore: FeatureSdkCore,
    internal val delegate: AgentTracer.TracerAPI,
    internal val bundleWithRumEnabled: Boolean,
    private val spanLogger: MotadataSpanLogger,
    private val rumContextPropagator: RumContextPropagator = RumContextPropagator { sdkCore }
) : MotadataTracer {

    override fun buildSpan(instrumentationName: String, spanName: CharSequence): MotadataSpanBuilder = wrapSpan(
        delegate.buildSpan(instrumentationName, spanName)
    )

    @Suppress("DEPRECATION")
    override fun buildSpan(spanName: CharSequence): MotadataSpanBuilder = wrapSpan(delegate.buildSpan(spanName))

    override fun addScopeListener(scopeListener: MotadataScopeListener) {
        delegate.addScopeListener(MotadataScopeListenerAdapter(scopeListener))
    }

    override fun propagate(): MotadataPropagation = MotadataPropagationAdapter(
        internalLogger = sdkCore.internalLogger,
        delegate = delegate.propagate()
    )

    override fun activeSpan(): MotadataSpan? = delegate.activeSpan()?.let { MotadataSpanAdapter(it, spanLogger) }

    override fun activateSpan(span: MotadataSpan): MotadataScope? = (span as? MotadataSpanAdapter)?.let {
        MotadataScopeAdapter(
            delegate.activateSpan(span.delegate, ScopeSource.INSTRUMENTATION) ?: return null
        )
    }

    internal fun activateSpan(span: MotadataSpan, asyncPropagating: Boolean): MotadataScope? {
        return (span as? MotadataSpanAdapter)
            ?.let { delegate.activateSpan(it.delegate, ScopeSource.INSTRUMENTATION, asyncPropagating) }
            ?.let { MotadataScopeAdapter(it) }
    }

    private fun wrapSpan(span: AgentTracer.SpanBuilder) =
        MotadataSpanBuilderAdapter(span, spanLogger)
            .withRumContextIfNeeded()

    private fun MotadataSpanBuilder.withRumContextIfNeeded() = apply {
        if (bundleWithRumEnabled) injectRumContext(rumContextPropagator)
    }
}
