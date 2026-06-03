/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.motadata.android.trace.internal

import android.content.Context
import com.motadata.android.api.feature.Feature
import com.motadata.android.api.feature.FeatureSdkCore
import com.motadata.android.api.feature.StorageBackedFeature
import com.motadata.android.api.net.RequestFactory
import com.motadata.android.api.storage.FeatureStorageConfiguration
import com.motadata.android.trace.InternalCoreWriterProvider
import com.motadata.android.trace.event.SpanEventMapper
import com.motadata.android.trace.internal.data.CoreTraceWriter
import com.motadata.android.trace.internal.domain.event.CoreTracerSpanToSpanEventMapper
import com.motadata.android.trace.internal.domain.event.SpanEventMapperWrapper
import com.motadata.android.trace.internal.domain.event.SpanEventSerializer
import com.motadata.android.trace.internal.net.TracesRequestFactory
import com.datadog.trace.common.writer.NoOpWriter
import com.datadog.trace.common.writer.Writer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Tracing feature class, which needs to be registered with Datadog SDK instance.
 */
internal class TracingFeature(
    private val sdkCore: FeatureSdkCore,
    customEndpointUrl: String?,
    internal val spanEventMapper: SpanEventMapper,
    internal val networkInfoEnabled: Boolean
) : InternalCoreWriterProvider, StorageBackedFeature {

    internal var coreTracerDataWriter: Writer = NoOpWriter()
    internal val initialized = AtomicBoolean(false)

    // region Feature

    override val name: String = Feature.TRACING_FEATURE_NAME

    override fun onInitialize(appContext: Context) {
        coreTracerDataWriter = createDataWriter(sdkCore)
        initialized.set(true)
    }

    override val requestFactory: RequestFactory by lazy {
        TracesRequestFactory(
            customEndpointUrl,
            sdkCore.internalLogger
        )
    }

    override val storageConfiguration: FeatureStorageConfiguration =
        FeatureStorageConfiguration.DEFAULT

    override fun onStop() {
        initialized.set(false)
    }

    // endregion

    // region InternalCoreWriterProvider

    override fun getCoreTracerWriter() = DatadogSpanWriterWrapper(coreTracerDataWriter)

    // endregion

    private fun createDataWriter(sdkCore: FeatureSdkCore): Writer {
        val internalLogger = sdkCore.internalLogger
        return CoreTraceWriter(
            sdkCore,
            ddSpanToSpanEventMapper = CoreTracerSpanToSpanEventMapper(networkInfoEnabled),
            eventMapper = SpanEventMapperWrapper(spanEventMapper, internalLogger),
            serializer = SpanEventSerializer(internalLogger),
            internalLogger = internalLogger
        )
    }
}
