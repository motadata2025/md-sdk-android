/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.motadata.android.tv.sample

import android.app.Application
import android.util.Log
import com.motadata.android.Motadata
import com.motadata.android.MotadataSite
import com.motadata.android.core.configuration.BatchSize
import com.motadata.android.core.configuration.Configuration
import com.motadata.android.core.configuration.UploadFrequency
import com.motadata.android.core.sampling.RateBasedSampler
import com.motadata.android.log.Logger
import com.motadata.android.log.Logs
import com.motadata.android.log.LogsConfiguration
import com.motadata.android.okhttp.MotadataEventListener
import com.motadata.android.okhttp.MotadataInterceptor
import com.motadata.android.okhttp.trace.TracingInterceptor
import com.motadata.android.privacy.TrackingConsent
import com.motadata.android.rum.GlobalRumMonitor
import com.motadata.android.rum.Rum
import com.motadata.android.rum.RumConfiguration
import com.motadata.android.rum.tracking.ActivityViewTrackingStrategy
import com.motadata.android.sessionreplay.ImagePrivacy
import com.motadata.android.sessionreplay.SessionReplay
import com.motadata.android.sessionreplay.SessionReplayConfiguration
import com.motadata.android.sessionreplay.SystemRequirementsConfiguration
import com.motadata.android.sessionreplay.TextAndInputPrivacy
import com.motadata.android.sessionreplay.TouchPrivacy
import com.motadata.android.sessionreplay.material.MaterialExtensionSupport
import com.motadata.android.timber.MotadataTree
import com.motadata.android.tv.sample.net.OkHttpDownloader
import okhttp3.OkHttpClient
import org.schabi.newpipe.extractor.NewPipe
import timber.log.Timber

/**
 * The main [Application] for the sample TV project.
 */
class TvSampleApplication : Application() {

    internal lateinit var okHttpClient: OkHttpClient

    override fun onCreate() {
        super.onCreate()
        initializeDatadog()
        initializeTimber()
        initializeOkHttp()
        initializeNewPipe()
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

        val sessionReplayConfig = createSessionReplayConfiguration()
        SessionReplay.enable(sessionReplayConfig)

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

    private fun createSessionReplayConfiguration(): SessionReplayConfiguration {
        return SessionReplayConfiguration.Builder(FULL_SAMPLING_RATE)
            .setImagePrivacy(ImagePrivacy.MASK_ALL)
            .setTouchPrivacy(TouchPrivacy.SHOW)
            .setTextAndInputPrivacy(TextAndInputPrivacy.MASK_SENSITIVE_INPUTS)
            .addExtensionSupport(MaterialExtensionSupport())
            .setSystemRequirements(SystemRequirementsConfiguration.NONE)
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

    @Suppress("TooGenericExceptionCaught", "CheckInternal")
    private fun initializeTimber() {
        val logger = Logger.Builder()
            .setName("timber")
            .setNetworkInfoEnabled(true)
            .setLogcatLogsEnabled(true)
            .build()

        Timber.plant(MotadataTree(logger))
    }

    private fun initializeOkHttp() {
        okHttpClient = OkHttpClient.Builder()
            .addInterceptor(
                MotadataInterceptor.Builder(emptyMap())
                    .setTraceSampler(RateBasedSampler(FULL_SAMPLING_RATE))
                    .build()
            )
            .addNetworkInterceptor(
                TracingInterceptor.Builder(emptyMap())
                    .setTraceSampler(RateBasedSampler(FULL_SAMPLING_RATE))
                    .build()
            )
            .eventListenerFactory(MotadataEventListener.Factory())
            .build()
    }

    private fun initializeNewPipe() {
        NewPipe.init(OkHttpDownloader(okHttpClient))
    }

    companion object {
        private const val FULL_SAMPLING_RATE = 100f
    }
}
