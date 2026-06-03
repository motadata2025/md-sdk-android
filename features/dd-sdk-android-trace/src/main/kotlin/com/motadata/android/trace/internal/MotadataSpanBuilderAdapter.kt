/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */
package com.motadata.android.trace.internal

import com.motadata.android.trace.api.span.MotadataSpan
import com.motadata.android.trace.api.span.MotadataSpanBuilder
import com.motadata.android.trace.api.span.MotadataSpanContext
import com.motadata.android.trace.api.span.MotadataSpanLink
import com.datadog.trace.bootstrap.instrumentation.api.AgentTracer

internal class MotadataSpanBuilderAdapter(
    private val delegate: AgentTracer.SpanBuilder,
    private val spanLogger: MotadataSpanLogger
) : MotadataSpanBuilder {

    override fun ignoreActiveSpan() = apply { delegate.ignoreActiveSpan() }

    override fun start(): MotadataSpan = MotadataSpanAdapter(delegate.start(), spanLogger)

    override fun withOrigin(origin: String?) = apply { delegate.withOrigin(origin) }

    override fun withStartTimestamp(micros: Long) = apply { delegate.withStartTimestamp(micros) }

    override fun withTag(key: String, value: Double?): MotadataSpanBuilder = apply { delegate.withTag(key, value) }

    override fun withTag(key: String, value: Long?): MotadataSpanBuilder = apply { delegate.withTag(key, value) }

    override fun withTag(key: String, value: Any?): MotadataSpanBuilder = apply { delegate.withTag(key, value) }

    override fun withLink(link: MotadataSpanLink): MotadataSpanBuilder = apply {
        delegate.withLink(MotadataSpanLinkAdapter(link))
    }

    override fun withResourceName(resourceName: String?): MotadataSpanBuilder = apply {
        delegate.withResourceName(resourceName)
    }

    override fun withParentContext(parentContext: MotadataSpanContext?): MotadataSpanBuilder = apply {
        if (parentContext is MotadataSpanContextAdapter) delegate.asChildOf(parentContext.delegate)
    }

    override fun withParentSpan(parentSpan: MotadataSpan?) = withParentContext(parentSpan?.context())
}
