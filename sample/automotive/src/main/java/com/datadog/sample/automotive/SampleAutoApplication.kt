/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.sample.automotive

import android.app.Application
import android.util.Log
import com.motadata.android.Motadata
import com.motadata.android.MotadataSite
import com.motadata.android.core.configuration.BatchSize
import com.motadata.android.core.configuration.Configuration
import com.motadata.android.core.configuration.UploadFrequency
import com.motadata.android.log.Logs
import com.motadata.android.log.LogsConfiguration
import com.motadata.android.privacy.TrackingConsent
import com.motadata.android.rum.GlobalRumMonitor
import com.motadata.android.rum.Rum
import com.motadata.android.rum.RumConfiguration
import com.motadata.android.rum.tracking.ActivityViewTrackingStrategy

@Suppress("UndocumentedPublicClass")
class SampleAutoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initializeDatadog()
    }

    private fun initializeDatadog() {
        Motadata.setVerbosity(Log.VERBOSE)
        Motadata.initialize(
            this,
            createDatadogConfiguration(),
            TrackingConsent.GRANTED
        )

        val rumConfig = createRumConfiguration()
        Rum.enable(rumConfig)

        val logsConfig = LogsConfiguration.Builder().build()
        Logs.enable(logsConfig)

        GlobalRumMonitor.get().debug = true
    }

    private fun createRumConfiguration(): RumConfiguration {
        return RumConfiguration.Builder(BuildConfig.DD_RUM_APPLICATION_ID)
            .useViewTrackingStrategy(
                ActivityViewTrackingStrategy(true)
            )
            .setTelemetrySampleRate(FULL_SAMPLING_RATE)
            .trackUserInteractions()
            .build()
    }
    private fun createDatadogConfiguration(): Configuration {
        return Configuration.Builder(
            clientToken = BuildConfig.DD_CLIENT_TOKEN,
            env = "test",
            variant = ""
        )
            .useSite(MotadataSite.US1)
            .setBatchSize(BatchSize.SMALL)
            .setUploadFrequency(UploadFrequency.FREQUENT)
            .build()
    }

    private companion object {
        private const val FULL_SAMPLING_RATE = 100f
    }
}
