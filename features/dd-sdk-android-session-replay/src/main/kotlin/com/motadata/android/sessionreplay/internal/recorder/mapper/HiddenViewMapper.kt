/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.motadata.android.sessionreplay.internal.recorder.mapper

import android.view.View
import com.motadata.android.api.InternalLogger
import com.motadata.android.sessionreplay.model.MobileSegment
import com.motadata.android.sessionreplay.recorder.MappingContext
import com.motadata.android.sessionreplay.recorder.mapper.WireframeMapper
import com.motadata.android.sessionreplay.utils.AsyncJobStatusCallback
import com.motadata.android.sessionreplay.utils.ViewBoundsResolver
import com.motadata.android.sessionreplay.utils.ViewIdentifierResolver

internal class HiddenViewMapper(
    val viewIdentifierResolver: ViewIdentifierResolver,
    val viewBoundsResolver: ViewBoundsResolver
) : WireframeMapper<View> {
    override fun map(
        view: View,
        mappingContext: MappingContext,
        asyncJobStatusCallback: AsyncJobStatusCallback,
        internalLogger: InternalLogger
    ): List<MobileSegment.Wireframe> {
        val id = viewIdentifierResolver.resolveChildUniqueIdentifier(view, HIDDEN_KEY_NAME)
            ?: return emptyList()

        val density = mappingContext.systemInformation.screenDensity
        val viewGlobalBounds = viewBoundsResolver.resolveViewGlobalBounds(view, density)

        return listOf(
            MobileSegment.Wireframe.PlaceholderWireframe(
                id = id,
                x = viewGlobalBounds.x,
                y = viewGlobalBounds.y,
                width = viewGlobalBounds.width,
                height = viewGlobalBounds.height,
                label = HIDDEN_VIEW_PLACEHOLDER_TEXT
            )
        )
    }

    internal companion object {
        internal const val HIDDEN_VIEW_PLACEHOLDER_TEXT = "Hidden"
        private const val HIDDEN_KEY_NAME = "hidden"
    }
}
