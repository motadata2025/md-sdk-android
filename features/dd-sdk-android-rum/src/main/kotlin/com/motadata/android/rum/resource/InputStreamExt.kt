/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.motadata.android.rum.resource

import com.motadata.android.Datadog
import com.motadata.android.api.SdkCore
import com.motadata.android.rum.RumMonitor
import java.io.InputStream

/**
 * Allow the [RumMonitor] to track this [InputStream] as a RUM Resource.
 *
 * @param url the url to be associated with this resource
 * @param sdkCore the SDK instance to use. If not provided, default instance will be used.
 */
fun InputStream.asRumResource(url: String, sdkCore: SdkCore = Datadog.getInstance()): InputStream {
    return RumResourceInputStream(this, url, sdkCore)
}
