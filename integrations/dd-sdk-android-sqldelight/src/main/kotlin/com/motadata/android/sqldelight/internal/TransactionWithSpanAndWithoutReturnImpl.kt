/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.motadata.android.sqldelight.internal

import com.motadata.android.sqldelight.TransactionWithSpanAndWithoutReturn
import com.motadata.android.trace.api.span.DatadogSpan
import com.squareup.sqldelight.TransactionWithoutReturn

internal class TransactionWithSpanAndWithoutReturnImpl(
    private val span: DatadogSpan,
    private val transaction: TransactionWithoutReturn
) : DatadogSpan by span,
    TransactionWithSpanAndWithoutReturn,
    TransactionWithoutReturn by transaction
