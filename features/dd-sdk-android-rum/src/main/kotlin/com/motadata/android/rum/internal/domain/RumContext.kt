/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.motadata.android.rum.internal.domain

import com.motadata.android.rum.internal.domain.scope.RumSessionScope
import com.motadata.android.rum.internal.domain.scope.RumViewType
import java.util.UUID

internal data class RumContext(
    val applicationId: String = NULL_UUID,
    val sessionId: String = NULL_UUID,
    val isSessionActive: Boolean = false,
    val viewId: String? = null,
    val viewName: String? = null,
    val viewUrl: String? = null,
    val actionId: String? = null,
    val sessionState: RumSessionScope.State = RumSessionScope.State.NOT_TRACKED,
    val sessionStartReason: RumSessionScope.StartReason = RumSessionScope.StartReason.USER_APP_LAUNCH,
    val viewType: RumViewType = RumViewType.NONE,
    val syntheticsTestId: String? = null,
    val syntheticsResultId: String? = null,
    val viewTimestamp: Long = 0L,
    val viewTimestampOffset: Long = 0L,
    val hasReplay: Boolean = false,
    // Server-corrected epoch-ms at which the current session started. Used to emit
    // session.created and context._timing on every event.
    val sessionStartTimestampMs: Long = 0L
) {

    fun toMap(): Map<String, Any?> {
        return mapOf(
            APPLICATION_ID to applicationId,
            SESSION_ID to sessionId,
            SESSION_ACTIVE to isSessionActive,
            SESSION_STATE to sessionState.asString,
            SESSION_START_REASON to sessionStartReason.asString,
            VIEW_ID to viewId,
            VIEW_NAME to viewName,
            VIEW_URL to viewUrl,
            VIEW_TYPE to viewType.asString,
            ACTION_ID to actionId,
            SYNTHETICS_TEST_ID to syntheticsTestId,
            SYNTHETICS_RESULT_ID to syntheticsResultId,
            VIEW_TIMESTAMP to viewTimestamp,
            HAS_REPLAY to hasReplay,
            VIEW_TIMESTAMP_OFFSET to viewTimestampOffset,
            SESSION_START_TIMESTAMP to sessionStartTimestampMs
        )
    }

    companion object {
        val NULL_UUID = UUID(0, 0).toString()
        const val SAMPLE_ALL_RATE: Float = 100f

        // be careful when changing values below, they may be indirectly referenced (as string
        // literal) from other modules
        const val APPLICATION_ID = "application_id"
        const val SESSION_ID = "session_id"
        const val SESSION_ACTIVE = "session_active"
        const val SESSION_STATE = "session_state"
        const val SESSION_START_REASON = "session_start_reason"
        const val VIEW_ID = "view_id"
        const val VIEW_NAME = "view_name"
        const val VIEW_URL = "view_url"
        const val VIEW_TYPE = "view_type"
        const val ACTION_ID = "action_id"
        const val SYNTHETICS_TEST_ID = "synthetics_test_id"
        const val SYNTHETICS_RESULT_ID = "synthetics_result_id"
        const val HAS_REPLAY = "view_has_replay"
        const val VIEW_TIMESTAMP = "view_timestamp"
        const val VIEW_TIMESTAMP_OFFSET = "view_timestamp_offset"
        const val SESSION_START_TIMESTAMP = "session_start_timestamp"

        // context._timing keys + nanos-per-ms factor (backend reads relativeTime as nanoseconds).
        const val TIMING_CONTEXT_KEY = "_timing"
        const val TIMING_NAVIGATION_START_KEY = "navigationStart"
        const val TIMING_RELATIVE_TIME_KEY = "relativeTime"
        private const val NANOS_IN_MILLI = 1_000_000L

        /**
         * Builds the `_timing` context object emitted on every event (Option A: navigationStart ==
         * session start). `relativeTime` is in nanoseconds (the unit the intake expects).
         */
        fun buildTimingContext(eventTimestampMs: Long, sessionStartTimestampMs: Long): Map<String, Long> {
            return mapOf(
                TIMING_NAVIGATION_START_KEY to sessionStartTimestampMs,
                TIMING_RELATIVE_TIME_KEY to (eventTimestampMs - sessionStartTimestampMs) * NANOS_IN_MILLI
            )
        }

        fun fromFeatureContext(featureContext: Map<String, Any?>): RumContext {
            val applicationId = featureContext[APPLICATION_ID] as? String
            val sessionId = featureContext[SESSION_ID] as? String
            val isSessionActive = featureContext[SESSION_ACTIVE] as? Boolean
            val sessionState = RumSessionScope.State.fromString(
                featureContext[SESSION_STATE] as? String
            )
            val sessionStartReason = RumSessionScope.StartReason.fromString(
                featureContext[SESSION_START_REASON] as? String
            )
            val viewId = featureContext[VIEW_ID] as? String
            val viewName = featureContext[VIEW_NAME] as? String
            val viewUrl = featureContext[VIEW_URL] as? String
            val viewType = RumViewType.fromString(featureContext[VIEW_TYPE] as? String)
            val actionId = featureContext[ACTION_ID] as? String
            val syntheticsTestId = featureContext[SYNTHETICS_TEST_ID] as? String
            val syntheticsResultId = featureContext[SYNTHETICS_RESULT_ID] as? String
            val hasReplay = featureContext[HAS_REPLAY] as? Boolean ?: false
            val viewTimestamp = featureContext[VIEW_TIMESTAMP] as? Long ?: 0L
            val viewTimestampOffset = featureContext[VIEW_TIMESTAMP_OFFSET] as? Long ?: 0L
            val sessionStartTimestampMs = featureContext[SESSION_START_TIMESTAMP] as? Long ?: 0L

            return RumContext(
                applicationId = applicationId ?: NULL_UUID,
                sessionId = sessionId ?: NULL_UUID,
                isSessionActive = isSessionActive ?: false,
                sessionState = sessionState ?: RumSessionScope.State.NOT_TRACKED,
                sessionStartReason = sessionStartReason ?: RumSessionScope.StartReason.USER_APP_LAUNCH,
                viewId = viewId,
                viewName = viewName,
                viewUrl = viewUrl,
                viewType = viewType ?: RumViewType.NONE,
                actionId = actionId,
                syntheticsTestId = syntheticsTestId,
                syntheticsResultId = syntheticsResultId,
                viewTimestamp = viewTimestamp,
                viewTimestampOffset = viewTimestampOffset,
                hasReplay = hasReplay,
                sessionStartTimestampMs = sessionStartTimestampMs
            )
        }
    }
}
