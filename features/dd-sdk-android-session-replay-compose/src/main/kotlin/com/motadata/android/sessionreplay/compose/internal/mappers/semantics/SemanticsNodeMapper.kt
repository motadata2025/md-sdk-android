/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.motadata.android.sessionreplay.compose.internal.mappers.semantics

import androidx.compose.ui.semantics.SemanticsNode
import com.motadata.android.sessionreplay.compose.internal.data.SemanticsWireframe
import com.motadata.android.sessionreplay.compose.internal.data.UiContext
import com.motadata.android.sessionreplay.utils.AsyncJobStatusCallback

internal interface SemanticsNodeMapper {

    fun map(
        semanticsNode: SemanticsNode,
        parentContext: UiContext,
        asyncJobStatusCallback: AsyncJobStatusCallback
    ): SemanticsWireframe?
}
