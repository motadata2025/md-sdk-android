/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.motadata.android.sessionreplay.compose.internal.data

import androidx.compose.ui.unit.Density
import com.motadata.android.sessionreplay.ImagePrivacy
import com.motadata.android.sessionreplay.TextAndInputPrivacy
import com.motadata.android.sessionreplay.utils.ImageWireframeHelper

internal data class UiContext(
    val parentContentColor: String?,
    val density: Float,
    val textAndInputPrivacy: TextAndInputPrivacy,
    val imagePrivacy: ImagePrivacy,
    val isInUserInputLayout: Boolean = false,
    val imageWireframeHelper: ImageWireframeHelper
) {
    val composeDensity: Density
        get() = Density(density)
}
