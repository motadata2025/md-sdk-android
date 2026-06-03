/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */
package com.motadata.android.okhttp.internal

import com.motadata.android.api.InternalLogger
import com.motadata.android.api.instrumentation.network.HttpRequestInfo
import com.motadata.android.api.instrumentation.network.HttpResponseInfo
import okhttp3.Request
import okhttp3.Response

internal fun Request.toHttpRequestInfo() = OkHttpRequestInfo(this)
internal fun HttpRequestInfo.toOkHttpRequest(): Request? = (this as? OkHttpRequestInfo)?.originalRequest
internal fun HttpResponseInfo.toOkHttpResponse(): Response? = (this as? OkHttpResponseInfo)?.originalResponse
internal fun Response.toHttpResponseInfo(internalLogger: InternalLogger) = OkHttpResponseInfo(this, internalLogger)
