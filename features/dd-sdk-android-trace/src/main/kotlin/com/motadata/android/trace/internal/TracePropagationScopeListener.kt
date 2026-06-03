/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */
package com.motadata.android.trace.internal

import com.motadata.android.api.feature.FeatureSdkCore
import com.motadata.android.trace.api.scope.DatadogScopeListener
import com.motadata.android.trace.api.tracer.DatadogTracer

internal class TracePropagationScopeListener(
    private val sdkCore: FeatureSdkCore,
    private val datadogTracer: DatadogTracer
) : DatadogScopeListener {
    override fun afterScopeActivated() {
        val activeSpanContext = datadogTracer.activeSpan()?.context()
        if (activeSpanContext != null) {
            val activeSpanId = activeSpanContext.spanId.toString()
            val activeTraceId = activeSpanContext.traceId.toHexString()
            sdkCore.addActiveTraceToContext(activeTraceId, activeSpanId)
        }
    }

    override fun afterScopeClosed() {
        sdkCore.removeActiveTraceFromContext()
    }
}
