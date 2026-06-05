/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.motadata.android

import androidx.annotation.FloatRange
import androidx.annotation.WorkerThread
import com.motadata.android.api.SdkCore
import com.motadata.android.api.feature.Feature
import com.motadata.android.api.feature.FeatureScope
import com.motadata.android.api.feature.FeatureSdkCore
import com.motadata.android.core.configuration.Configuration
import com.motadata.android.core.internal.MotadataCore
import com.motadata.android.internal.telemetry.InternalTelemetryEvent
import com.motadata.android.lint.InternalApi

/**
 * This class exposes internal methods that are used by other Motadata modules and cross-platform
 * frameworks. It is not meant for public use.
 *
 * DO NOT USE this class or its methods if you are not working on the internals of the Motadata SDK
 * or one of the cross-platform frameworks.
 *
 * Methods, members, and functionality of this class  are subject to change without notice, as they
 * are not considered part of the public interface of the Motadata SDK.
 */
@InternalApi
@Suppress(
    "UndocumentedPublicClass",
    "UndocumentedPublicFunction",
    "UndocumentedPublicProperty",
    "ClassName",
    "ClassNaming",
    "VariableNaming"
)
class _InternalProxy internal constructor(
    private val sdkCore: SdkCore
) {
    @Suppress("StringLiteralDuplication")
    class _TelemetryProxy internal constructor(private val sdkCore: SdkCore) {

        private val rumFeature: FeatureScope?
            get() {
                return (sdkCore as? FeatureSdkCore)?.getFeature(Feature.RUM_FEATURE_NAME)
            }

        fun debug(message: String) {
            val telemetryEvent = InternalTelemetryEvent.Log.Debug(
                message = message,
                additionalProperties = null
            )
            rumFeature?.sendEvent(telemetryEvent)
        }

        fun error(message: String, throwable: Throwable? = null) {
            val telemetryEvent = InternalTelemetryEvent.Log.Error(
                message = message,
                error = throwable
            )
            rumFeature?.sendEvent(telemetryEvent)
        }

        fun error(message: String, stack: String?, kind: String?) {
            val telemetryEvent = InternalTelemetryEvent.Log.Error(
                message = message,
                stacktrace = stack,
                kind = kind
            )
            rumFeature?.sendEvent(telemetryEvent)
        }
    }

    @Suppress("PropertyName")
    val _telemetry: _TelemetryProxy = _TelemetryProxy(sdkCore)

    fun setMetricTelemetrySampleRateBypass(
        @FloatRange(from = 0.0, to = 100.0) sampleRate: Float
    ) {
        (sdkCore as? MotadataCore)?.coreFeature?.metricTelemetrySampleRateBypass = sampleRate
    }

    fun setCustomAppVersion(version: String) {
        val coreFeature = (sdkCore as? MotadataCore)?.coreFeature
        coreFeature?.packageVersionProvider?.version = version
    }

    @WorkerThread
    fun flushAndShutdownExecutors() {
        Motadata.flushAndShutdownExecutors()
    }

    companion object {
        // Now a public API: Configuration.Builder.allowClearTextHttp(). This bridge is kept
        // for backward compatibility (flutter/react-native plugins + integration tests that
        // call it through the proxy, e.g. OkHttp MockWebServer is HTTP based).
        fun allowClearTextHttp(builder: Configuration.Builder): Configuration.Builder {
            return builder.allowClearTextHttp()
        }
    }
}
