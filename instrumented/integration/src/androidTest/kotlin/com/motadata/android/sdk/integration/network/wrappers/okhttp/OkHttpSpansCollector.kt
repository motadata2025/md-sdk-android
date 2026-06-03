/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */
package com.motadata.android.sdk.integration.network.wrappers.okhttp

import com.motadata.android.okhttp.trace.TracedRequestListener
import com.motadata.android.trace.api.span.MotadataSpan
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.CopyOnWriteArrayList

class OkHttpSpansCollector : TracedRequestListener {

    val spans = CopyOnWriteArrayList<MotadataSpan>()

    override fun onRequestIntercepted(
        request: Request,
        span: MotadataSpan,
        response: Response?,
        throwable: Throwable?
    ) {
        spans.add(span)
    }
}
