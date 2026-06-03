/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.motadata.android.rum.internal.startup

import com.motadata.android.core.InternalSdkCore
import com.motadata.android.rum.MdRumContentProvider

internal interface RumAppStartupTelemetryReporter {
    fun reportTTID(info: RumTTIDInfo, indexInSession: Int)

    companion object {
        fun create(sdkCore: InternalSdkCore): RumAppStartupTelemetryReporter {
            return RumAppStartupTelemetryReporterImpl(
                internalLogger = sdkCore.internalLogger,
                appStartupTimeNs = sdkCore.appStartTimeNs,
                contentProviderCreationTimeNs = MdRumContentProvider.createTimeNs,
                processStartImportance = MdRumContentProvider.processImportance
            )
        }
    }
}
