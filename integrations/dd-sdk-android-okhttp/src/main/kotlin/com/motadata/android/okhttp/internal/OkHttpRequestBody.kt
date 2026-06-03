/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */
package com.motadata.android.okhttp.internal

import com.motadata.android.api.instrumentation.network.HttpRequestBody
import okhttp3.RequestBody

internal data class OkHttpRequestBody(val body: RequestBody) : HttpRequestBody
