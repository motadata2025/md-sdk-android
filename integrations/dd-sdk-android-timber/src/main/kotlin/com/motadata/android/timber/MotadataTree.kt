/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.motadata.android.timber

import com.motadata.android.Motadata
import com.motadata.android.api.SdkCore
import com.motadata.android.log.Logger
import timber.log.Timber

/**
 * An implementation of a [Timber.Tree], forwarding all logs to the provided [Logger].
 *
 * @param logger the logger to use with Timber.
 */
class MotadataTree(
    private val logger: Logger
) : Timber.Tree() {

    /**
     * Creates a [Timber.Tree] with a default [Logger] having a minimum log priority
     * for Motadata logs set to specified value.
     *
     * See [Logger.Builder.setRemoteLogThreshold] for details.
     *
     * @param minLogPriority Minimum log threshold (priority) to be sent to the Motadata servers.
     * @param sdkCore SDK instance to bind to. If not provided, default instance will be used.
     */
    @Suppress("unused")
    @JvmOverloads
    constructor(minLogPriority: Int, sdkCore: SdkCore = Motadata.getInstance()) :
        this(
            Logger.Builder(sdkCore)
                .setRemoteLogThreshold(minLogPriority)
                .build()
        )

    init {
        logger.addTag("android:timber")
    }

    override fun log(
        priority: Int,
        tag: String?,
        message: String,
        t: Throwable?
    ) {
        val attributes = if (tag != null) {
            mapOf(TIMBER_TAG_ATTRIBUTE to tag)
        } else {
            emptyMap()
        }
        logger.log(priority, message, t, attributes)
    }

    private companion object {
        const val TIMBER_TAG_ATTRIBUTE = "timber.tag"
    }
}
