/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.motadata.android.rum.internal.utils

import com.motadata.android.api.context.MotadataContext
import com.motadata.android.rum.RumAttributes

internal fun buildDDTagsString(context: MotadataContext): String {
    return buildString {
        with(context) {
            append("${RumAttributes.SERVICE_NAME}:$service").append(",")
            append("${RumAttributes.APPLICATION_VERSION}:$version").append(",")
            append("${RumAttributes.SDK_VERSION}:$sdkVersion").append(",")
            append("${RumAttributes.ENV}:$env")

            if (variant.isNotEmpty()) {
                append(",").append("${RumAttributes.VARIANT}:$variant")
            }
        }
    }
}
