/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */
package com.motadata.android.trace.api.propagation

import com.motadata.android.trace.api.span.MotadataSpanContext

/**
 * A no-operation implementation of the [MotadataPropagation] interface.
 *
 * This implementation is intended as a placeholder making possible to create other NoOp.* classes.
 */
// TODO RUM-10573 - replace with @NoOpImplementation when method-level generics will be supported in noopfactory
class NoOpMotadataPropagation : MotadataPropagation {

    override fun <C> inject(
        context: MotadataSpanContext,
        carrier: C,
        setter: (carrier: C, key: String, value: String) -> Unit
    ) = Unit // Do nothing

    override fun <C> extract(
        carrier: C,
        getter: (carrier: C, classifier: (String, String) -> Boolean) -> Unit
    ): MotadataSpanContext? = null
}
