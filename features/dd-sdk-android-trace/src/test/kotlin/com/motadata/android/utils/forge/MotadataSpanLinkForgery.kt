/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */
package com.motadata.android.utils.forge

import com.motadata.android.trace.api.span.MotadataSpanLink
import com.motadata.android.trace.api.trace.MotadataTraceId
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.ForgeryFactory

internal class MotadataSpanLinkForgery : ForgeryFactory<MotadataSpanLink> {
    override fun getForgery(forge: Forge): MotadataSpanLink = object : MotadataSpanLink {
        override val spanId: Long = forge.aLong()
        override val sampled: Boolean = forge.aBool()
        override val traceStrace: String = forge.aString()
        override val attributes: Map<String, String> = forge.aMap { aString() to aString() }
        override val traceId: MotadataTraceId = forge.getForgery()
    }
}
