/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.motadata.android.api.net

import com.motadata.android.api.context.MotadataContext
import com.motadata.android.api.storage.RawBatchEvent

/**
 * Factory used to build requests from the batches stored.
 */
fun interface RequestFactory {

    /**
     * Creates a request for the given batch.
     * @param context Motadata SDK context.
     * @param executionContext Information about the execution context this request in case of a previous retry.
     * This information is specific to a certain batch and will be reset for the next batch in case of a drop or
     * a successful request.
     * @param batchData Raw data of the batch.
     * @param batchMetadata Raw metadata of the batch.
     * @throws [Exception] in case the request could not be created.
     */
    fun create(
        context: MotadataContext,
        executionContext: RequestExecutionContext,
        batchData: List<RawBatchEvent>,
        batchMetadata: ByteArray?
    ): Request?

    companion object {
        /**
         * application/json content type.
         */
        const val CONTENT_TYPE_JSON: String = "application/json"

        /**
         * text/plain;charset=UTF-8 content type.
         */
        const val CONTENT_TYPE_TEXT_UTF8: String = "text/plain;charset=UTF-8"

        /**
         * Motadata API key header.
         */
        const val HEADER_API_KEY: String = "MD-API-KEY"

        /**
         * Motadata Event Platform Origin header, e.g. android, flutter, etc.
         */
        const val HEADER_EVP_ORIGIN: String = "MD-EVP-ORIGIN"

        /**
         * Motadata Event Platform Origin version header, e.g. SDK version.
         */
        const val HEADER_EVP_ORIGIN_VERSION: String = "MD-EVP-ORIGIN-VERSION"

        /**
         * Motadata Request ID header, used for debugging purposes.
         */
        const val HEADER_REQUEST_ID: String = "MD-REQUEST-ID"

        /**
         * Motadata source query parameter name.
         */
        const val QUERY_PARAM_SOURCE: String = "mdsource"

        /**
         * Motadata tags query parameter name.
         */
        const val QUERY_PARAM_TAGS: String = "mdtags"

        /**
         * Motadata API key query parameter name. The intake authenticates on this query parameter
         * (the [HEADER_API_KEY] header is still sent, but the server reads the key from the query).
         */
        const val QUERY_PARAM_API_KEY: String = "md-api-key"

        /**
         * Motadata Idempotency key header, used to offer more insight into the request retry statistics.
         */
        const val DD_IDEMPOTENCY_KEY: String = "MD-IDEMPOTENCY-KEY"
    }
}
