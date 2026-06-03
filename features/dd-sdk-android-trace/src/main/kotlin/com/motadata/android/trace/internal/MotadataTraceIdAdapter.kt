/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */
package com.motadata.android.trace.internal

import com.motadata.android.trace.api.trace.MotadataTraceId
import com.datadog.trace.api.DDTraceId

internal data class MotadataTraceIdAdapter(private val delegate: DDTraceId) : MotadataTraceId, DDTraceId() {
    override fun toLong(): Long = delegate.toLong()
    override fun toString(): String = delegate.toString()
    override fun toHexString(): String = delegate.toHexString()
    override fun toHighOrderLong(): Long = delegate.toHighOrderLong()
    override fun toHexStringPadded(size: Int): String = delegate.toHexStringPadded(size)
}
