/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.motadata.android.sessionreplay.material.internal

import com.motadata.android.api.InternalLogger
import com.motadata.android.sessionreplay.ImagePrivacy
import com.motadata.android.sessionreplay.model.MobileSegment
import com.motadata.android.sessionreplay.recorder.MappingContext
import com.motadata.android.sessionreplay.recorder.mapper.TextViewMapper
import com.motadata.android.sessionreplay.utils.AsyncJobStatusCallback
import com.motadata.android.sessionreplay.utils.ColorStringFormatter
import com.motadata.android.sessionreplay.utils.DrawableToColorMapper
import com.motadata.android.sessionreplay.utils.ViewBoundsResolver
import com.motadata.android.sessionreplay.utils.ViewIdentifierResolver
import com.google.android.material.chip.Chip

internal class ChipWireframeMapper(
    viewIdentifierResolver: ViewIdentifierResolver,
    colorStringFormatter: ColorStringFormatter,
    viewBoundsResolver: ViewBoundsResolver,
    drawableToColorMapper: DrawableToColorMapper
) : TextViewMapper<Chip>(
    viewIdentifierResolver,
    colorStringFormatter,
    viewBoundsResolver,
    drawableToColorMapper
) {
    override fun map(
        view: Chip,
        mappingContext: MappingContext,
        asyncJobStatusCallback: AsyncJobStatusCallback,
        internalLogger: InternalLogger
    ): List<MobileSegment.Wireframe> {
        val wireframes = mutableListOf<MobileSegment.Wireframe>()

        val viewGlobalBounds = viewBoundsResolver.resolveViewGlobalBounds(
            view,
            mappingContext.systemInformation.screenDensity
        )
        val density = mappingContext.systemInformation.screenDensity
        val drawableBounds = view.chipDrawable.bounds
        val backgroundWireframe =
            mappingContext.imageWireframeHelper.createImageWireframeByDrawable(
                view = view,
                // Background drawable doesn't need to be masked.
                imagePrivacy = ImagePrivacy.MASK_NONE,
                currentWireframeIndex = 0,
                x = viewGlobalBounds.x + drawableBounds.left.toLong().densityNormalized(density),
                y = viewGlobalBounds.y + drawableBounds.top.toLong().densityNormalized(density),
                width = view.chipDrawable.intrinsicWidth,
                height = view.chipDrawable.intrinsicHeight,
                usePIIPlaceholder = false,
                drawable = view.chipDrawable,
                customResourceIdCacheKey = null,
                asyncJobStatusCallback = asyncJobStatusCallback
            )
        backgroundWireframe?.let {
            wireframes.add(it)
        }
        // Text wireframe
        wireframes.add(super.createTextWireframe(view, mappingContext, viewGlobalBounds))
        return wireframes.toList()
    }
}
