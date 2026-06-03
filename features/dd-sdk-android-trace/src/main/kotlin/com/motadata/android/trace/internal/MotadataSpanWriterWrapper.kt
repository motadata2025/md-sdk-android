/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */
package com.motadata.android.trace.internal

import com.motadata.android.trace.api.span.MotadataSpanWriter
import com.datadog.trace.common.writer.Writer

internal class MotadataSpanWriterWrapper(internal val delegate: Writer) : MotadataSpanWriter
