/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.motadata.android.wear.sample

import android.app.Application
import android.util.Log
import com.motadata.android.Motadata
import com.motadata.android.MotadataSite
import com.motadata.android.core.configuration.Configuration
import com.motadata.android.log.Logs
import com.motadata.android.log.LogsConfiguration
import com.motadata.android.privacy.TrackingConsent
import com.motadata.android.rum.Rum
import com.motadata.android.rum.RumConfiguration
import com.motadata.android.rum.tracking.ActivityViewTrackingStrategy
import com.motadata.android.trace.MotadataTracing
import com.motadata.android.trace.GlobalMotadataTracer
import com.motadata.android.trace.Trace
import com.motadata.android.trace.TraceConfiguration
import com.motadata.android.trace.opentelemetry.MotadataOpenTelemetry
import io.opentelemetry.api.GlobalOpenTelemetry
import timber.log.Timber

/**
 * The main [Application] for the sample WearOs project.
 */
class WearApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initializeDatadog()
    }

    @Suppress("MagicNumber")
    private fun initializeDatadog() {
        Motadata.setVerbosity(Log.VERBOSE)

        val sdkCore = Motadata.initialize(
            this,
            createDatadogConfiguration(),
            TrackingConsent.GRANTED
        )

        Rum.enable(
            RumConfiguration.Builder(BuildConfig.DD_RUM_APPLICATION_ID)
                .setTelemetrySampleRate(100f)
                .useViewTrackingStrategy(ActivityViewTrackingStrategy(true))
                .trackUserInteractions()
                .trackLongTasks(250L)
                .apply {
                    if (BuildConfig.DD_OVERRIDE_RUM_URL.isNotBlank()) {
                        useCustomEndpoint(BuildConfig.DD_OVERRIDE_RUM_URL)
                    }
                }
                .build()
        )

        Logs.enable(
            LogsConfiguration.Builder()
                .apply {
                    if (BuildConfig.DD_OVERRIDE_LOGS_URL.isNotBlank()) {
                        useCustomEndpoint(BuildConfig.DD_OVERRIDE_LOGS_URL)
                    }
                }
                .build()
        )

        Trace.enable(
            TraceConfiguration.Builder()
                .apply {
                    if (BuildConfig.DD_OVERRIDE_TRACES_URL.isNotBlank()) {
                        useCustomEndpoint(BuildConfig.DD_OVERRIDE_TRACES_URL)
                    }
                }
                .build()
        )

        Motadata.setUserInfo(
            id = "wear 42",
            name = null,
            email = null
        )

        GlobalMotadataTracer.registerIfAbsent(
            MotadataTracing.newTracerBuilder(checkNotNull(sdkCore))
                .withServiceName(BuildConfig.APPLICATION_ID)
                .build()
        )

        GlobalOpenTelemetry.set(
            MotadataOpenTelemetry(BuildConfig.APPLICATION_ID)
        )
    }

    private fun createDatadogConfiguration(): Configuration {
        val configBuilder = Configuration.Builder(
            clientToken = BuildConfig.DD_CLIENT_TOKEN,
            env = BuildConfig.BUILD_TYPE,
            variant = BuildConfig.FLAVOR
        )

        try {
            configBuilder.useSite(MotadataSite.valueOf(BuildConfig.DD_SITE_NAME))
        } catch (e: IllegalArgumentException) {
            Timber.e("Error setting site to ${BuildConfig.DD_SITE_NAME}")
        }

        return configBuilder.build()
    }
}
