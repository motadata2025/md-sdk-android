/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.motadata.android.sdk.integration.network.rules

import androidx.test.platform.app.InstrumentationRegistry
import com.motadata.android.Motadata
import com.motadata.android._InternalProxy
import com.motadata.android.api.SdkCore
import com.motadata.android.core.configuration.Configuration
import com.motadata.android.privacy.TrackingConsent
import com.motadata.android.sdk.integration.network.utils.TestEchoWebServer
import com.motadata.android.trace.MotadataTracing
import com.motadata.android.trace.GlobalMotadataTracer
import com.motadata.android.trace.Trace
import com.motadata.android.trace.TraceConfiguration
import fr.xgouchet.elmyr.Forge
import org.junit.rules.ExternalResource

/**
 * JUnit Rule for network instrumentation integration tests.
 *
 * Manages [com.motadata.android.sdk.integration.network.utils.TestEchoWebServer] and Motadata SDK initialization.
 */
internal class NetworkInstrumentationTestRule : ExternalResource() {

    private var sdkCore: SdkCore? = null
    private val mockWebServer = TestEchoWebServer()

    val forge = Forge()

    /**
     * Base URL for HTTP requests to the mock server.
     */
    val baseUrl: String
        get() = mockWebServer.baseUrl

    override fun before() {
        mockWebServer.start()
        setupDatadogSdk()
    }

    override fun after() {
        GlobalMotadataTracer.clear()
        Motadata.stopInstance()
        mockWebServer.shutdown()

        InstrumentationRegistry
            .getInstrumentation()
            .targetContext
            .cacheDir
            .deleteRecursively()
    }

    private fun setupDatadogSdk() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val sdkConfig = Configuration.Builder(
            clientToken = FAKE_CLIENT_TOKEN,
            env = FAKE_ENV
        )
            .apply { _InternalProxy.allowClearTextHttp(this) }
            .build()

        sdkCore = checkNotNull(
            Motadata.initialize(context, sdkConfig, TrackingConsent.GRANTED)
        ) { "Failed to initialize Motadata SDK" }

        Trace.enable(
            TraceConfiguration.Builder()
                .build()
        )

        GlobalMotadataTracer.registerIfAbsent(
            MotadataTracing.newTracerBuilder()
                .withPartialFlushMinSpans(1)
                .build()
        )
    }

    companion object {
        private const val FAKE_CLIENT_TOKEN = "fake-token"
        private const val FAKE_ENV = "integration-test"
    }
}
