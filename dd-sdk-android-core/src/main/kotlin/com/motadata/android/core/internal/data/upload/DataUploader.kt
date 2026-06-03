/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.motadata.android.core.internal.data.upload

import com.motadata.android.api.context.MotadataContext
import com.motadata.android.api.storage.RawBatchEvent
import com.motadata.android.core.internal.persistence.BatchId

internal interface DataUploader {
    fun upload(
        context: MotadataContext,
        batch: List<RawBatchEvent>,
        batchMeta: ByteArray?,
        batchId: BatchId? = null
    ): UploadStatus
}
