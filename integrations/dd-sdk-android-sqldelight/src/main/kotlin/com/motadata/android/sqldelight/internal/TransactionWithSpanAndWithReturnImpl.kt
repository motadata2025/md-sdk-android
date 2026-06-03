/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.motadata.android.sqldelight.internal

import com.motadata.android.sqldelight.TransactionWithSpanAndWithReturn
import com.motadata.android.trace.api.span.MotadataSpan
import com.squareup.sqldelight.TransactionWithReturn

internal class TransactionWithSpanAndWithReturnImpl<R>(
    private val span: MotadataSpan,
    private val transaction: TransactionWithReturn<R>
) : TransactionWithSpanAndWithReturn<R>, MotadataSpan by span, TransactionWithReturn<R> by transaction
