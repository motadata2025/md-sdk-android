/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.motadata.android.sessionreplay.compose.internal.mappers.semantics

import androidx.compose.ui.semantics.SemanticsNode
import com.motadata.android.sessionreplay.compose.internal.data.SemanticsWireframe
import com.motadata.android.sessionreplay.compose.internal.data.UiContext
import com.motadata.android.sessionreplay.compose.internal.utils.SemanticsUtils
import com.motadata.android.sessionreplay.model.MobileSegment
import com.motadata.android.sessionreplay.utils.AsyncJobStatusCallback
import com.motadata.android.sessionreplay.utils.ColorStringFormatter

internal class TabSemanticsNodeMapper(
    colorStringFormatter: ColorStringFormatter,
    semanticsUtils: SemanticsUtils = SemanticsUtils()
) : AbstractSemanticsNodeMapper(colorStringFormatter, semanticsUtils) {

    override fun map(
        semanticsNode: SemanticsNode,
        parentContext: UiContext,
        asyncJobStatusCallback: AsyncJobStatusCallback
    ): SemanticsWireframe {
        val globalBounds = resolveBounds(semanticsNode)
        val shapeStyle =
            MobileSegment.ShapeStyle(backgroundColor = parentContext.parentContentColor)
        val wireframe = MobileSegment.Wireframe.ShapeWireframe(
            id = semanticsNode.id.toLong(),
            x = globalBounds.x,
            y = globalBounds.y,
            width = globalBounds.width,
            height = globalBounds.height,
            shapeStyle = shapeStyle
        )
        return SemanticsWireframe(
            wireframes = listOf(wireframe),
            uiContext = parentContext
        )
    }
}
