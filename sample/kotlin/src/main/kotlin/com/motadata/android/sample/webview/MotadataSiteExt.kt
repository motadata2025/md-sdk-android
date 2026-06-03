/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.motadata.android.sample.webview

import com.motadata.android.MotadataSite
import com.motadata.android.sample.BuildConfig
import timber.log.Timber

internal val BROWSER_SITE: String
    get() {
        return try {
            MotadataSite.valueOf(BuildConfig.DD_SITE_NAME)
        } catch (e: IllegalArgumentException) {
            Timber.e("Error setting site to ${BuildConfig.DD_SITE_NAME}")
            null
        }.browserSite()
    }

private fun MotadataSite?.browserSite(): String {
    return when (this) {
        MotadataSite.US1,
        MotadataSite.STAGING,
        null -> "datadoghq.com"

        MotadataSite.US3 -> "us3.datadoghq.com"
        MotadataSite.US5 -> "us5.datadoghq.com"
        MotadataSite.EU1 -> "datadoghq.eu"
        MotadataSite.AP1 -> "ap1.datadoghq.com"
        MotadataSite.AP2 -> "ap2.datadoghq.com"
        MotadataSite.US1_FED -> "ddog-gov.com"
        MotadataSite.US2_FED -> "us2.ddog-gov.com"
    }
}
