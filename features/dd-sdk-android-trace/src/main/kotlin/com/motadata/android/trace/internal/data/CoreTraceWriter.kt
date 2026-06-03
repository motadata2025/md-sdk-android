/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.motadata.android.trace.internal.data

import androidx.annotation.WorkerThread
import com.motadata.android.api.InternalLogger
import com.motadata.android.api.context.MotadataContext
import com.motadata.android.api.feature.Feature
import com.motadata.android.api.feature.FeatureSdkCore
import com.motadata.android.api.storage.EventBatchWriter
import com.motadata.android.api.storage.EventType
import com.motadata.android.api.storage.RawBatchEvent
import com.motadata.android.event.EventMapper
import com.motadata.android.event.NoOpEventMapper
import com.motadata.android.trace.internal.RumContextPropagator
import com.motadata.android.trace.internal.RumContextPropagator.Companion.extractRumContext
import com.motadata.android.trace.internal.domain.event.ContextAwareMapper
import com.motadata.android.trace.internal.storage.ContextAwareSerializer
import com.motadata.android.trace.model.SpanEvent
import com.datadog.trace.api.sampling.PrioritySampling
import com.datadog.trace.common.writer.Writer
import com.datadog.trace.core.DDSpan
import java.util.Locale

internal class CoreTraceWriter(
    private val sdkCore: FeatureSdkCore,
    internal val ddSpanToSpanEventMapper: ContextAwareMapper<DDSpan, SpanEvent>,
    internal val eventMapper: EventMapper<SpanEvent> = NoOpEventMapper(),
    private val serializer: ContextAwareSerializer<SpanEvent>,
    private val internalLogger: InternalLogger,
    private val rumContextPropagator: RumContextPropagator = RumContextPropagator { sdkCore }
) : Writer {

    // region Writer
    override fun start() {
        // NO - OP
    }

    override fun write(trace: List<DDSpan>?) {
        if (trace == null) return
        sdkCore.getFeature(Feature.TRACING_FEATURE_NAME)
            ?.withWriteContext { datadogContext, writeScope ->
                val writeSpans = trace
                    .filter { it.getTraceSamplingPriority() !in DROP_SAMPLING_PRIORITIES }
                    .map { it.extractRumContext(rumContextPropagator) }
                // TODO RUM-4092 Add the capability in the serializer to handle multiple spans in one payload
                writeScope {
                    writeSpans
                        .forEach { span ->
                            @Suppress("ThreadSafety") // called in the worker context
                            writeSpan(datadogContext, it, span)
                        }
                }
            }
    }

    override fun flush(): Boolean {
        // NO - OP
        return true
    }

    override fun incrementDropCounts(p0: Int) {
        // NO - OP
    }

    override fun close() {
        // NO - OP
    }
    // endregion

    @WorkerThread
    private fun writeSpan(
        datadogContext: MotadataContext,
        writer: EventBatchWriter,
        span: DDSpan
    ) {
        val spanEvent = ddSpanToSpanEventMapper.map(datadogContext, span)
        val mapped = eventMapper.map(spanEvent) ?: return
        try {
            val serialized = serializer
                .serialize(datadogContext, mapped)
                ?.toByteArray(Charsets.UTF_8) ?: return
            synchronized(this) {
                writer.write(RawBatchEvent(data = serialized), batchMetadata = null, eventType = EventType.DEFAULT)
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
            internalLogger.log(
                InternalLogger.Level.ERROR,
                listOf(InternalLogger.Target.USER, InternalLogger.Target.TELEMETRY),
                { ERROR_SERIALIZING.format(Locale.US, mapped.javaClass.simpleName) },
                e
            )
        }
    }

    companion object {
        internal const val ERROR_SERIALIZING = "Error serializing %s model"
        internal val DROP_SAMPLING_PRIORITIES = setOf(
            PrioritySampling.SAMPLER_DROP.toInt(),
            PrioritySampling.USER_DROP.toInt()
        )
    }
}
