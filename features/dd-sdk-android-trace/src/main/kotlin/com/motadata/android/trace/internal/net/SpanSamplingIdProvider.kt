/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.motadata.android.trace.internal.net

import com.motadata.android.log.LogAttributes
import com.motadata.android.trace.api.span.MotadataSpan

private const val HEX_RADIX = 16

internal object SpanSamplingIdProvider {

    fun provideId(span: MotadataSpan): ULong {
        val context = span.context()
        val sessionId = context.tags[LogAttributes.RUM_SESSION_ID] as? String

        // for a UUID with value aaaaaaaa-bbbb-Mccc-Nddd-1234567890ab
        // we use as the input id the last part : 0x1234567890ab
        val sessionIdToken = sessionId?.split('-')
            ?.lastOrNull()
            ?.toLongOrNull(HEX_RADIX)
            ?.toULong()

        return sessionIdToken ?: context.traceId.toLong().toULong()
    }
}
