/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.motadata.android.trace.coroutines

import com.motadata.android.trace.api.span.MotadataSpan
import kotlinx.coroutines.CoroutineScope

/**
 * An object that implements both [MotadataSpan] and [CoroutineScope].
 */
interface CoroutineScopeSpan : CoroutineScope, MotadataSpan
