/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.motadata.android.sessionreplay.internal.recorder.mapper

import android.view.View
import androidx.annotation.UiThread
import com.motadata.android.api.InternalLogger
import com.motadata.android.sessionreplay.model.MobileSegment
import com.motadata.android.sessionreplay.recorder.MappingContext
import com.motadata.android.sessionreplay.recorder.mapper.BaseWireframeMapper
import com.motadata.android.sessionreplay.utils.AsyncJobStatusCallback
import com.motadata.android.sessionreplay.utils.ColorStringFormatter
import com.motadata.android.sessionreplay.utils.DrawableToColorMapper
import com.motadata.android.sessionreplay.utils.ViewBoundsResolver
import com.motadata.android.sessionreplay.utils.ViewIdentifierResolver

internal class ViewWireframeMapper(
    viewIdentifierResolver: ViewIdentifierResolver,
    colorStringFormatter: ColorStringFormatter,
    viewBoundsResolver: ViewBoundsResolver,
    drawableToColorMapper: DrawableToColorMapper
) : BaseWireframeMapper<View>(
    viewIdentifierResolver,
    colorStringFormatter,
    viewBoundsResolver,
    drawableToColorMapper
) {
    @UiThread
    override fun map(
        view: View,
        mappingContext: MappingContext,
        asyncJobStatusCallback: AsyncJobStatusCallback,
        internalLogger: InternalLogger
    ): List<MobileSegment.Wireframe> {
        val viewGlobalBounds = viewBoundsResolver.resolveViewGlobalBounds(
            view,
            mappingContext.systemInformation.screenDensity
        )
        val shapeStyle = view.background?.let { resolveShapeStyle(it, view.alpha, internalLogger) }

        if (shapeStyle != null) {
            return listOf(
                MobileSegment.Wireframe.ShapeWireframe(
                    resolveViewId(view),
                    viewGlobalBounds.x,
                    viewGlobalBounds.y,
                    viewGlobalBounds.width,
                    viewGlobalBounds.height,
                    shapeStyle = shapeStyle,
                    border = null
                )
            )
        } else {
            return emptyList()
        }
    }
}
