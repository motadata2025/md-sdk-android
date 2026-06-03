/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */
package com.motadata.android.utils.forge

import com.motadata.android.trace.api.span.MotadataSpan
import com.motadata.android.trace.api.span.MotadataSpanContext
import com.motadata.android.trace.internal.MotadataTraceIdAdapter
import com.datadog.trace.api.DDTraceId
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.ForgeryFactory
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

internal class MotadataSpanForgeryFactory : ForgeryFactory<MotadataSpan> {

    override fun getForgery(forge: Forge): MotadataSpan {
        val rootSpan = forge.createSpan(context = forge.getForgery<MotadataSpanContext>())
        return forge.createSpan(context = forge.getForgery<MotadataSpanContext>(), localSpan = rootSpan)
    }

    private fun Forge.createSpan(context: MotadataSpanContext, localSpan: MotadataSpan? = null) = mock<MotadataSpan> {
        on { isRootSpan } doReturn aBool()
        on { isError } doReturn aBool()
        on { resourceName } doReturn aString()
        on { serviceName } doReturn aString()
        on { operationName } doReturn aString()
        on { traceId } doReturn MotadataTraceIdAdapter(getForgery<DDTraceId>())
        on { parentSpanId } doReturn aLong()
        on { samplingPriority } doReturn anInt()
        on { durationNano } doReturn aLong()
        on { startTimeNanos } doReturn aLong()
        on { localRootSpan } doReturn localSpan
        on { context() } doReturn context
    }
}
