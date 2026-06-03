/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */
package com.motadata.android.utils.forge

import com.motadata.android.trace.api.trace.MotadataTraceId
import com.motadata.android.trace.internal.MotadataTraceIdAdapter
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.ForgeryFactory

class MotadataTraceIdForgeryFactory : ForgeryFactory<MotadataTraceId> {
    override fun getForgery(forge: Forge): MotadataTraceId {
        return MotadataTraceIdAdapter(forge.getForgery())
    }
}
