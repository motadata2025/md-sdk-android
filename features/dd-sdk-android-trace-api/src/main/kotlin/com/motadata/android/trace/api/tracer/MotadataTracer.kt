/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */
package com.motadata.android.trace.api.tracer

import com.motadata.android.trace.api.propagation.MotadataPropagation
import com.motadata.android.trace.api.propagation.NoOpMotadataPropagation
import com.motadata.android.trace.api.scope.MotadataScope
import com.motadata.android.trace.api.scope.MotadataScopeListener
import com.motadata.android.trace.api.span.MotadataSpan
import com.motadata.android.trace.api.span.MotadataSpanBuilder
import com.datadog.tools.annotation.NoOpImplementation

/**
 * [MotadataTracer] is a simple, thin interface for span creation and propagation across arbitrary transports.
 */
@NoOpImplementation(publicNoOpImplementation = true)
interface MotadataTracer {
    /**
     * Retrieves the currently active span in the context of the tracer.
     *
     * @return the active [MotadataSpan].
     */
    fun activeSpan(): MotadataSpan?

    /**
     * Provides an implementation of the [MotadataPropagation] interface to be used for span propagation.
     *
     * @return An instance of [MotadataPropagation]
     */
    fun propagate(): MotadataPropagation = NoOpMotadataPropagation()

    /**
     * Activates the provided span within the current context of the tracer.
     * Once activated, the span becomes the currently active span, and any operations
     * requiring an active span will use this one until it is explicitly deactivated.
     *
     * @param span The span to be activated. Represents the logical unit of work being traced.
     * @return An instance of [MotadataScope] representing the activated scope.
     */
    fun activateSpan(span: MotadataSpan): MotadataScope?

    /**
     * Creates a new span builder instance with the specified span name.
     *
     * @param spanName The name of the span to be built. Represents the operation being performed.
     * @return An instance of [MotadataSpanBuilder] to allow further configuration of the span.
     */
    fun buildSpan(spanName: CharSequence): MotadataSpanBuilder

    /**
     * Creates a new span builder instance with the specified instrumentation and span names.
     *
     * @param instrumentationName The name of the instrumentation associated with the span.
     * @param spanName The name of the span to be built. Represents the operation being performed.
     * @return An instance of [MotadataSpanBuilder] to allow further configuration of the span.
     */
    fun buildSpan(instrumentationName: String, spanName: CharSequence): MotadataSpanBuilder

    /**
     * Adds a listener to be notified when a scope is activated or closed.
     *
     * @param scopeListener The listener to be added. It defines the actions
     * to be executed after a scope is activated or closed.
     */
    fun addScopeListener(scopeListener: MotadataScopeListener)
}
