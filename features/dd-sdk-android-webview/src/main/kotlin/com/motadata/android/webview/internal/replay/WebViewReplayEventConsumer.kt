/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.motadata.android.webview.internal.replay

import com.motadata.android.api.InternalLogger
import com.motadata.android.api.context.MotadataContext
import com.motadata.android.api.feature.Feature
import com.motadata.android.api.feature.FeatureSdkCore
import com.motadata.android.api.storage.DataWriter
import com.motadata.android.api.storage.EventType
import com.motadata.android.webview.internal.WebViewEventConsumer
import com.motadata.android.webview.internal.rum.WebViewRumEventContextProvider
import com.motadata.android.webview.internal.rum.domain.RumContext
import com.google.gson.JsonObject
import java.lang.IllegalStateException
import java.lang.NumberFormatException
import java.lang.UnsupportedOperationException

internal class WebViewReplayEventConsumer(
    private val sdkCore: FeatureSdkCore,
    internal val dataWriter: DataWriter<JsonObject>,
    private val contextProvider: WebViewRumEventContextProvider,
    internal val webViewReplayEventMapper: WebViewReplayEventMapper
) : WebViewEventConsumer<JsonObject> {

    override fun consume(event: JsonObject) {
        sdkCore.getFeature(WebViewReplayFeature.WEB_REPLAY_FEATURE_NAME)
            ?.withWriteContext(
                withFeatureContexts = setOf(
                    Feature.RUM_FEATURE_NAME,
                    Feature.SESSION_REPLAY_FEATURE_NAME
                )
            ) { datadogContext, writeScope ->
                val rumContext = contextProvider.getRumContext(datadogContext)
                val sessionReplayFeatureContext = datadogContext.featuresContext[
                    Feature.SESSION_REPLAY_FEATURE_NAME
                ]
                val sessionReplayEnabled = sessionReplayFeatureContext?.get(
                    SESSION_REPLAY_ENABLED_KEY
                ) as? Boolean ?: false
                if (rumContext != null &&
                    rumContext.sessionState == "TRACKED" &&
                    sessionReplayEnabled
                ) {
                    writeScope {
                        map(event, datadogContext, rumContext)?.let { mappedEvent ->
                            dataWriter.write(it, mappedEvent, EventType.DEFAULT)
                        }
                    }
                }
            }
    }

    private fun map(
        event: JsonObject,
        datadogContext: MotadataContext,
        rumContext: RumContext
    ): JsonObject? {
        try {
            return webViewReplayEventMapper.mapEvent(event, rumContext, datadogContext)
        } catch (e: ClassCastException) {
            sdkCore.internalLogger.log(
                InternalLogger.Level.ERROR,
                listOf(InternalLogger.Target.MAINTAINER, InternalLogger.Target.TELEMETRY),
                { JSON_PARSING_ERROR_MESSAGE },
                e
            )
        } catch (e: NumberFormatException) {
            sdkCore.internalLogger.log(
                InternalLogger.Level.ERROR,
                listOf(InternalLogger.Target.MAINTAINER, InternalLogger.Target.TELEMETRY),
                { JSON_PARSING_ERROR_MESSAGE },
                e
            )
        } catch (e: IllegalStateException) {
            sdkCore.internalLogger.log(
                InternalLogger.Level.ERROR,
                listOf(InternalLogger.Target.MAINTAINER, InternalLogger.Target.TELEMETRY),
                { JSON_PARSING_ERROR_MESSAGE },
                e
            )
        } catch (e: UnsupportedOperationException) {
            sdkCore.internalLogger.log(
                InternalLogger.Level.ERROR,
                listOf(InternalLogger.Target.MAINTAINER, InternalLogger.Target.TELEMETRY),
                { JSON_PARSING_ERROR_MESSAGE },
                e
            )
        }
        return null
    }

    companion object {
        private const val RECORD_KEY = "record"
        const val SESSION_TRACKED_STATE = "TRACKED"
        val REPLAY_EVENT_TYPES = setOf(RECORD_KEY)
        const val JSON_PARSING_ERROR_MESSAGE =
            "The bundled web Replay event could not be deserialized"
        internal const val SESSION_REPLAY_ENABLED_KEY =
            "session_replay_is_enabled"
    }
}
