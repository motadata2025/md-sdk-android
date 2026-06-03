/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */
package com.motadata.android.trace.internal.net

import com.motadata.android.api.feature.Feature
import com.motadata.android.api.feature.FeatureSdkCore
import com.motadata.android.api.instrumentation.network.HttpRequestInfo
import com.motadata.android.core.sampling.Sampler
import com.motadata.android.trace.api.MotadataTracingConstants.PrioritySampling
import com.motadata.android.trace.api.MotadataTracingConstants.Tags
import com.motadata.android.trace.api.span.MotadataSpan
import com.motadata.android.trace.api.tracer.MotadataTracer
import com.motadata.android.trace.internal.ApmNetworkInstrumentation.Companion.AGENT_PSR_ATTRIBUTE
import com.motadata.android.trace.internal.ApmNetworkInstrumentation.Companion.ALL_IN_SAMPLE_RATE
import com.motadata.android.trace.internal.ApmNetworkInstrumentation.Companion.SPAN_NAME
import com.motadata.android.trace.internal.ApmNetworkInstrumentation.Companion.URL_QUERY_PARAMS_BLOCK_SEPARATOR
import com.motadata.android.trace.internal.ApmNetworkInstrumentation.Companion.ZERO_SAMPLE_RATE
import com.motadata.android.trace.internal._TraceInternalProxy.propagationHelper
import java.util.Locale

internal val FeatureSdkCore?.isRumEnabled: Boolean
    get() = this?.getFeature(Feature.RUM_FEATURE_NAME) != null

internal fun MotadataSpan.applyPriority(isSampled: Boolean, traceSampler: Sampler<MotadataSpan>) {
    val samplingPriority = if (isSampled) {
        PrioritySampling.SAMPLER_KEEP
    } else {
        PrioritySampling.SAMPLER_DROP
    }

    val spanContext = context()
    if (spanContext.setSamplingPriority(samplingPriority)) {
        spanContext.setMetric(
            AGENT_PSR_ATTRIBUTE,
            (traceSampler.getSampleRate() ?: ZERO_SAMPLE_RATE) / ALL_IN_SAMPLE_RATE
        )
    }
}

internal fun MotadataSpan.sample(request: HttpRequestInfo, traceSampler: Sampler<MotadataSpan>): Boolean {
    val samplingPriority = samplingPriority
    return if (samplingPriority != null) {
        samplingPriority > 0
    } else {
        propagationHelper.extractSamplingDecision(request) ?: traceSampler.sample(this)
    }
}

internal fun MotadataSpan.finishRumAware(isSampled: Boolean, canSendSpan: Boolean) {
    if (canSendSpan && isSampled) {
        finish()
    } else {
        drop()
    }
}

internal fun MotadataTracer.buildSpan(
    request: HttpRequestInfo,
    networkInstrumentationName: String,
    traceOrigin: String?
): MotadataSpan {
    val parentContext = propagationHelper.extractParentContext(this, request)

    val span = buildSpan(SPAN_NAME.format(Locale.US, networkInstrumentationName))
        .withOrigin(traceOrigin)
        .withParentContext(parentContext)
        .start()

    span.resourceName = request.url.substringBefore(URL_QUERY_PARAMS_BLOCK_SEPARATOR)
    span.setTag(Tags.KEY_HTTP_URL, request.url)
    span.setTag(Tags.KEY_HTTP_METHOD, request.method)
    span.setTag(Tags.KEY_SPAN_KIND, Tags.VALUE_SPAN_KIND_CLIENT)

    return span
}
