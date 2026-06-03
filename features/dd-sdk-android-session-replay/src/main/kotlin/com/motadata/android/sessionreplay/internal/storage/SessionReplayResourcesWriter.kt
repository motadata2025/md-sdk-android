/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.motadata.android.sessionreplay.internal.storage

import com.motadata.android.api.context.DatadogContext
import com.motadata.android.api.feature.Feature
import com.motadata.android.api.feature.FeatureSdkCore
import com.motadata.android.api.storage.EventType
import com.motadata.android.api.storage.RawBatchEvent
import com.motadata.android.sessionreplay.internal.processor.EnrichedResource
import com.motadata.android.sessionreplay.internal.processor.asBinaryMetadata

internal class SessionReplayResourcesWriter(
    private val sdkCore: FeatureSdkCore
) : ResourcesWriter {
    override fun write(enrichedResource: EnrichedResource) {
        sdkCore.getFeature(Feature.SESSION_REPLAY_RESOURCES_FEATURE_NAME)
            ?.withWriteContext(
                withFeatureContexts = setOf(Feature.RUM_FEATURE_NAME)
            ) { datadogContext, writeScope ->
                writeScope {
                    synchronized(this@SessionReplayResourcesWriter) {
                        val serializedMetadata = enrichedResource.asBinaryMetadata(datadogContext.rumApplicationId)
                        it.write(
                            event = RawBatchEvent(
                                data = enrichedResource.resource,
                                metadata = serializedMetadata
                            ),
                            batchMetadata = null,
                            eventType = EventType.DEFAULT
                        )
                    }
                }
            }
    }

    private val DatadogContext.rumApplicationId: String
        get() = (featuresContext[Feature.RUM_FEATURE_NAME]?.get("application_id") as? String).orEmpty()
}
