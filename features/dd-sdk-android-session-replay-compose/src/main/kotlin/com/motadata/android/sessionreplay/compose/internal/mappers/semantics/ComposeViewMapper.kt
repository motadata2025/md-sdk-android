/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.motadata.android.sessionreplay.compose.internal.mappers.semantics

import androidx.annotation.UiThread
import androidx.compose.ui.platform.ComposeView
import com.motadata.android.api.InternalLogger
import com.motadata.android.sessionreplay.compose.internal.utils.SemanticsUtils
import com.motadata.android.sessionreplay.model.MobileSegment
import com.motadata.android.sessionreplay.recorder.MappingContext
import com.motadata.android.sessionreplay.recorder.mapper.BaseWireframeMapper
import com.motadata.android.sessionreplay.utils.AsyncJobStatusCallback
import com.motadata.android.sessionreplay.utils.ColorStringFormatter
import com.motadata.android.sessionreplay.utils.DrawableToColorMapper
import com.motadata.android.sessionreplay.utils.ViewBoundsResolver
import com.motadata.android.sessionreplay.utils.ViewIdentifierResolver

internal class ComposeViewMapper(
    viewIdentifierResolver: ViewIdentifierResolver,
    colorStringFormatter: ColorStringFormatter,
    viewBoundsResolver: ViewBoundsResolver,
    drawableToColorMapper: DrawableToColorMapper,
    private val semanticsUtils: SemanticsUtils = SemanticsUtils(),
    private val rootSemanticsNodeMapper: RootSemanticsNodeMapper
) : BaseWireframeMapper<ComposeView>(
    viewIdentifierResolver,
    colorStringFormatter,
    viewBoundsResolver,
    drawableToColorMapper
) {
    @UiThread
    override fun map(
        view: ComposeView,
        mappingContext: MappingContext,
        asyncJobStatusCallback: AsyncJobStatusCallback,
        internalLogger: InternalLogger
    ): List<MobileSegment.Wireframe> {
        val density =
            mappingContext.systemInformation.screenDensity.let { if (it == 0.0f) 1.0f else it }
        return semanticsUtils.findRootSemanticsNode(view)?.let { node ->
            rootSemanticsNodeMapper.createComposeWireframes(
                node,
                density,
                mappingContext,
                asyncJobStatusCallback
            )
        } ?: emptyList()
    }
}
