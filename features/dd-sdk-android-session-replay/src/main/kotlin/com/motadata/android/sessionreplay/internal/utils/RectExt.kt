/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.motadata.android.sessionreplay.internal.utils

import android.graphics.Rect
import com.motadata.android.sessionreplay.model.MobileSegment

internal fun Rect.toWireframeClip() = MobileSegment.WireframeClip(
    this.top.toLong(),
    this.bottom.toLong(),
    this.left.toLong(),
    this.right.toLong()
)
