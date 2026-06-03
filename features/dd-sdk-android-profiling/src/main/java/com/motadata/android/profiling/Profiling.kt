/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.motadata.android.profiling

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.motadata.android.Motadata
import com.motadata.android.api.SdkCore
import com.motadata.android.api.feature.FeatureSdkCore
import com.motadata.android.internal.time.DefaultTimeProvider
import com.motadata.android.profiling.internal.NoOpProfiler
import com.motadata.android.profiling.internal.Profiler
import com.motadata.android.profiling.internal.ProfilingFeature
import com.motadata.android.profiling.internal.ProfilingStartReason
import com.motadata.android.profiling.internal.ProfilingStorage
import com.motadata.android.profiling.internal.perfetto.PerfettoProfiler
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * An entry point to Motadata Profiling feature.
 */
@ExperimentalProfilingApi
object Profiling {

    @Volatile
    internal var profiler: Profiler = NoOpProfiler()
    internal val isProfilerInitialized = AtomicBoolean(false)

    /**
     * Enables the profiling feature.
     *
     * @param configuration Configuration to use for the feature.
     * @param sdkCore SDK instance to register feature in. If not provided, default SDK instance
     * will be used.
     */
    @JvmStatic
    @JvmOverloads
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun enable(
        configuration: ProfilingConfiguration = ProfilingConfiguration.DEFAULT,
        sdkCore: SdkCore = Motadata.getInstance()
    ) {
        val featureSdkCore = sdkCore as FeatureSdkCore
        initializeProfiler()
        val profilingFeature = ProfilingFeature(
            sdkCore = featureSdkCore,
            configuration = configuration,
            profiler = profiler
        )
        featureSdkCore.registerFeature(profilingFeature)
    }

    /**
     * Start profiling with given SDK instances names.
     *
     * @param context application context
     * @param startReason reason to start a profiling session
     * @param additionalAttributes additional attributes to include in the profiling telemetry
     * @param sdkInstanceNames the set of the SDK instances name
     */
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    internal fun start(
        context: Context,
        startReason: ProfilingStartReason,
        additionalAttributes: Map<String, String>,
        sdkInstanceNames: Set<String>
    ) {
        initializeProfiler()
        profiler.start(context, startReason, additionalAttributes, sdkInstanceNames)
        ProfilingStorage.removeProfilingFlag(context, sdkInstanceNames)
    }

    /**
     * Start profiling for a given SDK instance.
     *
     * @param context application context
     * @param startReason reason to start a profiling session
     * @param additionalAttributes additional attributes to include in the profiling telemetry
     * @param sdkCore SDK instance to start profiling with. If not provided, default SDK instance.
     */
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    internal fun start(
        context: Context,
        startReason: ProfilingStartReason,
        additionalAttributes: Map<String, String>,
        sdkCore: SdkCore = Motadata.getInstance()
    ) {
        start(context, startReason, additionalAttributes, setOf(sdkCore.name))
    }

    /**
     * Stop profiling for a given SDK instance.
     *
     * @param sdkCore SDK instance to stop profiling. If not provided, default SDK instance.
     */
    internal fun stop(sdkCore: SdkCore = Motadata.getInstance()) {
        profiler.stop(sdkCore.name)
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun initializeProfiler() {
        if (!isProfilerInitialized.getAndSet(true)) {
            profiler = PerfettoProfiler(
                timeProvider = DefaultTimeProvider(),
                profilingExecutor = Executors.newSingleThreadExecutor()
            )
        }
    }
}
