/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.motadata.android.sessionreplay.internal.recorder.mapper

import com.motadata.android.sessionreplay.internal.async.RecordedDataQueueRefs
import com.motadata.android.sessionreplay.utils.AsyncJobStatusCallback

internal class QueueStatusCallback(
    private val recordedDataQueueRefs: RecordedDataQueueRefs
) : AsyncJobStatusCallback {

    override fun jobStarted() {
        recordedDataQueueRefs.incrementPendingJobs()
    }

    override fun jobFinished() {
        recordedDataQueueRefs.decrementPendingJobs()
        recordedDataQueueRefs.tryToConsumeItem()
    }
}
