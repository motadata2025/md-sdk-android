/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.motadata.android.sessionreplay.internal

import android.app.Application
import com.motadata.android.sessionreplay.internal.recorder.Recorder
import com.motadata.android.sessionreplay.internal.resources.ResourceDataStoreManager
import com.motadata.android.sessionreplay.internal.storage.RecordWriter
import com.motadata.android.sessionreplay.internal.storage.ResourcesWriter
import com.motadata.android.sessionreplay.internal.utils.RumContextProvider

internal fun interface RecorderProvider {
    fun provideSessionReplayRecorder(
        resourceDataStoreManager: ResourceDataStoreManager,
        resourceWriter: ResourcesWriter,
        recordWriter: RecordWriter,
        rumContextProvider: RumContextProvider,
        application: Application
    ): Recorder
}
