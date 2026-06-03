/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.motadata.android.ndk

import com.motadata.android.Motadata
import com.motadata.android.api.SdkCore
import com.motadata.android.api.feature.FeatureSdkCore
import com.motadata.android.ndk.internal.NdkCrashReportsFeature

/**
 * An entry point to Motadata NDK Crash Reports feature.
 */
object NdkCrashReports {

    /**
     * Enables a NDK Crash Reports feature.
     *
     * @param sdkCore SDK instance to register feature in. If not provided, default SDK instance
     * will be used.
     */
    @JvmOverloads
    @JvmStatic
    fun enable(sdkCore: SdkCore = Motadata.getInstance()) {
        val ndkCrashReportsFeature = NdkCrashReportsFeature(sdkCore as FeatureSdkCore)

        sdkCore.registerFeature(ndkCrashReportsFeature)
    }
}
