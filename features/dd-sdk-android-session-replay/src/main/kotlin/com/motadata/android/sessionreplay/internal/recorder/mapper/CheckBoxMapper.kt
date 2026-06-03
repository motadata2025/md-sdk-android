/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.motadata.android.sessionreplay.internal.recorder.mapper

import android.widget.CheckBox
import com.motadata.android.api.InternalLogger
import com.motadata.android.sessionreplay.recorder.mapper.TextViewMapper
import com.motadata.android.sessionreplay.utils.ColorStringFormatter
import com.motadata.android.sessionreplay.utils.DrawableToColorMapper
import com.motadata.android.sessionreplay.utils.ViewBoundsResolver
import com.motadata.android.sessionreplay.utils.ViewIdentifierResolver

internal open class CheckBoxMapper(
    textWireframeMapper: TextViewMapper<CheckBox>,
    viewIdentifierResolver: ViewIdentifierResolver,
    colorStringFormatter: ColorStringFormatter,
    viewBoundsResolver: ViewBoundsResolver,
    drawableToColorMapper: DrawableToColorMapper,
    internalLogger: InternalLogger
) : CheckableCompoundButtonMapper<CheckBox>(
    textWireframeMapper,
    viewIdentifierResolver,
    colorStringFormatter,
    viewBoundsResolver,
    drawableToColorMapper,
    internalLogger
)
