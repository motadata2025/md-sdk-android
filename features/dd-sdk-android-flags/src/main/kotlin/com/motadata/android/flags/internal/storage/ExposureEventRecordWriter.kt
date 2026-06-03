/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.motadata.android.flags.internal.storage

import com.motadata.android.api.feature.Feature
import com.motadata.android.api.feature.FeatureSdkCore
import com.motadata.android.api.storage.EventType
import com.motadata.android.api.storage.RawBatchEvent
import com.motadata.android.flags.model.ExposureEvent

internal class ExposureEventRecordWriter(private val sdkCore: FeatureSdkCore) : RecordWriter {
    override fun write(record: ExposureEvent) {
        sdkCore.getFeature(Feature.FLAGS_FEATURE_NAME)
            ?.withWriteContext { _, writeScope ->
                writeScope {
                    val serializedRecord = record.toJson().toString().toByteArray(Charsets.UTF_8)
                    val rawBatchEvent = RawBatchEvent(data = serializedRecord)
                    synchronized(this@ExposureEventRecordWriter) {
                        it.write(
                            event = rawBatchEvent,
                            batchMetadata = null,
                            eventType = EventType.DEFAULT
                        )
                    }
                }
            }
    }
}
