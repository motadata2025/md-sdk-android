/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */
package com.motadata.android.trace.internal

import com.motadata.android.trace.api.span.MotadataSpanLink
import com.datadog.trace.api.DDTraceId
import com.datadog.trace.bootstrap.instrumentation.api.AgentSpanLink
import com.datadog.trace.bootstrap.instrumentation.api.SpanLink
import com.datadog.trace.bootstrap.instrumentation.api.SpanLinkAttributes

internal class MotadataSpanLinkAdapter(delegate: MotadataSpanLink) :
    SpanLink(
        /* traceId */
        DDTraceId.fromHex(delegate.traceId.toHexString()),
        /* spanId */
        delegate.spanId,
        /* traceFlags */
        if (delegate.sampled) AgentSpanLink.SAMPLED_FLAG else AgentSpanLink.DEFAULT_FLAGS,
        /* traceState */
        delegate.traceStrace,
        /* attributes */
        SpanLinkAttributes.fromMap(delegate.attributes)
    )
