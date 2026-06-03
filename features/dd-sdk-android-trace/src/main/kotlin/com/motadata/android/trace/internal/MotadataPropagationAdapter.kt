/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.motadata.android.trace.internal

import com.motadata.android.api.InternalLogger
import com.motadata.android.trace.api.propagation.MotadataPropagation
import com.motadata.android.trace.api.span.MotadataSpanContext
import com.datadog.trace.bootstrap.instrumentation.api.AgentPropagation
import kotlin.reflect.KClass

internal class MotadataPropagationAdapter(
    private val internalLogger: InternalLogger,
    private val delegate: AgentPropagation
) : MotadataPropagation {

    override fun <C> inject(
        context: MotadataSpanContext,
        carrier: C,
        setter: (carrier: C, key: String, value: String) -> Unit
    ) {
        if (context !is MotadataSpanContextAdapter) {
            internalLogger.log(
                InternalLogger.Level.ERROR,
                InternalLogger.Target.USER,
                { constructErrorMessage(context::class) }
            )
            return
        }
        delegate.inject(context.delegate, carrier, setter)
    }

    override fun <C> extract(
        carrier: C,
        getter: (carrier: C, classifier: (String, String) -> Boolean) -> Unit
    ): MotadataSpanContext? {
        return delegate.extract(carrier) { car, cls -> getter(car, cls::accept) }
            ?.let { MotadataSpanContextAdapter(it) }
    }

    private fun constructErrorMessage(klass: KClass<*>) = "MotadataPropagationAdapter supports only" +
        " MotadataSpanContextAdapter instances for injection but ${klass.simpleName} is given"
}
