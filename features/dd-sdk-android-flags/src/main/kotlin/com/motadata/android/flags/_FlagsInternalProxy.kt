/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.motadata.android.flags

import com.motadata.android.flags.internal.MotadataFlagsClient
import com.motadata.android.flags.model.EvaluationContext
import com.motadata.android.flags.model.UnparsedFlag
import com.motadata.android.lint.InternalApi

/**
 * This class exposes internal methods that are used by other Motadata modules and cross platform
 * frameworks. It is not meant for public use.
 *
 * DO NOT USE this class or its methods if you are not working on the internals of the Motadata SDK
 * or one of the cross platform frameworks.
 *
 * Methods, members, and functionality of this class are subject to change without notice, as they
 * are not considered part of the public interface of the Motadata SDK.
 */
@InternalApi
@Suppress("ClassName", "UndocumentedPublicFunction")
class _FlagsInternalProxy(private val client: FlagsClient) {
    fun getFlagAssignmentsSnapshot(): Map<String, UnparsedFlag> = if (client is MotadataFlagsClient) {
        client.getFlagAssignmentsSnapshot()
    } else {
        emptyMap()
    }

    fun trackFlagSnapshotEvaluation(flagKey: String, flag: UnparsedFlag, context: EvaluationContext) {
        if (client is MotadataFlagsClient) {
            client.trackFlagSnapshotEvaluation(flagKey, flag, context)
        }
    }
}
