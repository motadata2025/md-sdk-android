/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */
package com.motadata.android.trace

import com.motadata.android.Motadata
import com.motadata.android.api.InternalLogger
import com.motadata.android.api.SdkCore
import com.motadata.android.api.feature.Feature
import com.motadata.android.api.feature.FeatureSdkCore
import com.motadata.android.trace.api.tracer.DatadogTracerBuilder
import com.motadata.android.trace.api.tracer.NoOpDatadogTracerBuilder
import com.motadata.android.trace.internal.DatadogSpanWriterWrapper
import com.motadata.android.trace.internal.DatadogTracerBuilderAdapter
import com.motadata.android.trace.internal._TraceInternalProxy
import com.datadog.trace.common.writer.NoOpWriter
import com.datadog.trace.core.CoreTracer

/**
 * Object responsible for providing tracing capabilities in the Motadata SDK.
 */
@SuppressWarnings("UndocumentedPublicFunction")
object DatadogTracing {
    /**
     * Creates and returns a new instance of [DatadogTracerBuilder]. This method initializes and configures the
     * tracer builder based on the provided SDK core instance and its setup. If certain required configurations
     * are missing, a no-operation tracer builder is returned instead.
     *
     * @param sdkCore An instance of [SdkCore], which provides configuration and features needed for initializing
     * the tracer builder.
     * @return A configured instance of [DatadogTracerBuilder], or a no-operation implementation if the necessary
     * configurations or features are unavailable.
     */
    @JvmStatic
    fun newTracerBuilder(sdkCore: SdkCore = Motadata.getInstance()): DatadogTracerBuilder = when {
        _TraceInternalProxy.testBuilderProvider != null -> {
            _TraceInternalProxy.testBuilderProvider as DatadogTracerBuilder
        }
        sdkCore !is FeatureSdkCore -> {
            NoOpDatadogTracerBuilder()
        }
        else -> {
            val internalLogger = sdkCore.internalLogger
            val tracingFeature = sdkCore.getFeature(Feature.TRACING_FEATURE_NAME)?.unwrap<Feature>()
            val internalCoreWriterProvider = tracingFeature as? InternalCoreWriterProvider
            val writer = (internalCoreWriterProvider?.getCoreTracerWriter() as? DatadogSpanWriterWrapper)?.delegate

            when {
                null == tracingFeature -> internalLogger.log(
                    InternalLogger.Level.ERROR,
                    InternalLogger.Target.USER,
                    { ErrorMessages.TRACING_NOT_ENABLED_ERROR_MESSAGE }
                )

                null == internalCoreWriterProvider -> internalLogger.log(
                    InternalLogger.Level.ERROR,
                    InternalLogger.Target.MAINTAINER,
                    { ErrorMessages.WRITER_PROVIDER_INTERFACE_NOT_IMPLEMENTED_ERROR_MESSAGE }
                )

                null == writer -> internalLogger.log(
                    InternalLogger.Level.ERROR,
                    InternalLogger.Target.USER,
                    {
                        ErrorMessages.buildWrongWrapperMessage(
                            internalCoreWriterProvider.getCoreTracerWriter().javaClass
                        )
                    }
                )

                sdkCore.service.isEmpty() -> internalLogger.log(
                    InternalLogger.Level.ERROR,
                    InternalLogger.Target.USER,
                    { ErrorMessages.DEFAULT_SERVICE_NAME_IS_MISSING_ERROR_MESSAGE }
                )
            }

            DatadogTracerBuilderAdapter(
                sdkCore = sdkCore,
                serviceName = sdkCore.service,
                delegate = CoreTracer.CoreTracerBuilder(sdkCore.internalLogger).writer(writer ?: NoOpWriter())
            )
        }
    }

    internal object ErrorMessages {
        const val TRACING_NOT_ENABLED_ERROR_MESSAGE: String =
            "You're trying to create an DatadogTracer instance, " +
                "but either the SDK was not initialized or the Tracing feature was " +
                "not registered. No tracing data will be sent."

        const val WRITER_PROVIDER_INTERFACE_NOT_IMPLEMENTED_ERROR_MESSAGE: String =
            "The Tracing feature is not implementing the InternalCoreWriterProvider interface." +
                " No tracing data will be sent."

        const val DEFAULT_SERVICE_NAME_IS_MISSING_ERROR_MESSAGE: String =
            "Default service name is missing during DatadogTracerBuilder creation, did you initialize SDK?"

        fun buildWrongWrapperMessage(cls: Class<*>?): String {
            return "You're trying to create an DatadogTracer instance, " +
                "but provided ${cls?.canonicalName} writer wrapper is not supported."
        }
    }
}
