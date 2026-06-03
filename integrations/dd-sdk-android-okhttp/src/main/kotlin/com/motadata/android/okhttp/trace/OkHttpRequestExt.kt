/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.motadata.android.okhttp.trace

import com.motadata.android.trace.api.span.MotadataSpan
import okhttp3.Request

/**
 * Set the parent for the [MotadataSpan] created around this OkHttp [Request].
 * @param span the parent [MotadataSpan]
 */
fun Request.Builder.parentSpan(span: MotadataSpan): Request.Builder {
    @Suppress("UnsafeThirdPartyFunctionCall") // Span can't be null
    tag(MotadataSpan::class.java, span)
    return this
}
