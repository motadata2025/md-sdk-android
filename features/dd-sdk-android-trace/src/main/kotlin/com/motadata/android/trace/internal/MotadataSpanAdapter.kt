/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */
package com.motadata.android.trace.internal

import com.motadata.android.trace.api.span.MotadataSpan
import com.motadata.android.trace.api.trace.MotadataTraceId
import com.datadog.trace.bootstrap.instrumentation.api.AgentSpan
import com.datadog.trace.core.DDSpan

internal class MotadataSpanAdapter(
    internal val delegate: AgentSpan,
    private val spanLogger: MotadataSpanLogger
) : MotadataSpan {

    override val isRootSpan: Boolean get() = delegate is DDSpan && delegate.isRootSpan

    override val traceId: MotadataTraceId get() = MotadataTraceIdAdapter(delegate.traceId)

    override val parentSpanId: Long? get() = (delegate as? DDSpan)?.parentId

    override val samplingPriority: Int? get() = delegate.traceSamplingPriority

    override val durationNano: Long get() = delegate.durationNano

    override val startTimeNanos: Long get() = delegate.startTime

    override val localRootSpan: MotadataSpan? get() = delegate.localRootSpan?.let { MotadataSpanAdapter(it, spanLogger) }

    override var isError: Boolean?
        get() = delegate.isError
        set(value) {
            if (value == null) return
            delegate.isError = value
        }

    override var resourceName: String?
        get() = delegate.resourceName?.toString()
        set(value) {
            delegate.resourceName = value
        }

    override var serviceName: String
        get() = delegate.serviceName
        set(value) {
            delegate.serviceName = value
        }

    override var operationName: String
        get() = delegate.operationName.toString()
        set(value) {
            delegate.operationName = value
        }

    override fun drop() = delegate.drop()

    override fun finish() = delegate.finish()

    override fun finish(finishMicros: Long) = delegate.finish(finishMicros)

    override fun context() = MotadataSpanContextAdapter(delegate.context())

    override fun setTag(tag: String?, value: String?) {
        delegate.setTag(tag, value)
    }

    override fun setTag(tag: String?, value: Boolean) {
        delegate.setTag(tag, value)
    }

    override fun setTag(tag: String?, value: Number?) {
        delegate.setTag(tag, value)
    }

    override fun setTag(tag: String?, value: Any?) {
        delegate.setTag(tag, value)
    }

    override fun getTag(tag: String?): Any? {
        return delegate.getTag(tag)
    }

    override fun setMetric(key: String, value: Int) {
        delegate.setMetric(key, value)
    }

    override fun setErrorMessage(message: String?) {
        delegate.setErrorMessage(message)
    }

    override fun addThrowable(throwable: Throwable) {
        delegate.addThrowable(throwable)
    }

    override fun logThrowable(throwable: Throwable) {
        spanLogger.log(throwable, this)
    }

    override fun logErrorMessage(message: String) {
        spanLogger.logErrorMessage(message, this)
    }

    override fun logMessage(message: String) {
        spanLogger.log(message, this)
    }

    override fun logAttributes(attributes: Map<String, Any>) {
        spanLogger.log(attributes, this)
    }

    internal fun addThrowable(throwable: Throwable, errorPriority: Byte) {
        delegate.addThrowable(throwable, errorPriority)
    }
}
