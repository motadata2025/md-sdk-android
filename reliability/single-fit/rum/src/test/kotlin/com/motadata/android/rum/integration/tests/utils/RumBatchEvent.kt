/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.motadata.android.rum.integration.tests.utils

import com.motadata.android.api.storage.RawBatchEvent
import com.google.gson.JsonElement

data class RumBatchEvent(
    val rumEvent: JsonElement,
    val batchEvent: RawBatchEvent
)
