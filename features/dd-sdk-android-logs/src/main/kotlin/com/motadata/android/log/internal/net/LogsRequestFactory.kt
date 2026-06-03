/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.motadata.android.log.internal.net

import com.motadata.android.api.InternalLogger
import com.motadata.android.api.context.MotadataContext
import com.motadata.android.api.net.Request
import com.motadata.android.api.net.RequestExecutionContext
import com.motadata.android.api.net.RequestFactory
import com.motadata.android.api.storage.RawBatchEvent
import com.motadata.android.core.internal.utils.join
import java.util.Locale
import java.util.UUID

/**
 * Request factory for the Logs feature.
 * @param customEndpointUrl URL of the Logs intake.
 * @param internalLogger logger to use.
 */
internal class LogsRequestFactory(
    internal val customEndpointUrl: String?,
    private val internalLogger: InternalLogger
) : RequestFactory {

    /** @inheritdoc */
    override fun create(
        context: MotadataContext,
        executionContext: RequestExecutionContext,
        batchData: List<RawBatchEvent>,
        batchMetadata: ByteArray?
    ): Request? {
        val requestId = UUID.randomUUID().toString()

        return Request(
            id = requestId,
            description = "Logs Request",
            url = buildUrl(context.source, context),
            headers = buildHeaders(
                requestId,
                context.clientToken,
                context.source,
                context.sdkVersion
            ),
            body = batchData.map { it.data }
                .join(
                    separator = PAYLOAD_SEPARATOR,
                    prefix = PAYLOAD_PREFIX,
                    suffix = PAYLOAD_SUFFIX,
                    internalLogger = internalLogger
                ),
            contentType = RequestFactory.CONTENT_TYPE_JSON
        )
    }

    private fun buildUrl(source: String, context: MotadataContext): String {
        val baseUrl = customEndpointUrl ?: (context.site.intakeEndpoint + "/api/v2/logs")
        return "%s?%s=%s"
            .format(
                Locale.US,
                baseUrl,
                RequestFactory.QUERY_PARAM_SOURCE,
                source
            )
    }

    private fun buildHeaders(
        requestId: String,
        clientToken: String,
        source: String,
        sdkVersion: String
    ): Map<String, String> {
        return mapOf(
            RequestFactory.HEADER_API_KEY to clientToken,
            RequestFactory.HEADER_EVP_ORIGIN to source,
            RequestFactory.HEADER_EVP_ORIGIN_VERSION to sdkVersion,
            RequestFactory.HEADER_REQUEST_ID to requestId
        )
    }

    companion object {
        private val PAYLOAD_SEPARATOR = ",".toByteArray(Charsets.UTF_8)
        private val PAYLOAD_PREFIX = "[".toByteArray(Charsets.UTF_8)
        private val PAYLOAD_SUFFIX = "]".toByteArray(Charsets.UTF_8)
    }
}
