/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */
package com.motadata.android.trace

import com.motadata.android.trace.api.tracer.MotadataTracer
import com.motadata.android.trace.api.tracer.NoOpMotadataTracer
import java.util.concurrent.atomic.AtomicReference

/**
 * A holder object for managing and retrieving a global instance of the [MotadataTracer].
 *
 * This object is used to share same instance of [MotadataTracer] across different integrations such as
 * `OkHttp`, Kotlin's coroutines, ect.
 */
object GlobalMotadataTracer {

    private val instance = AtomicReference<MotadataTracer?>()

    /**
     * Registers the provided tracer as the global tracer if no tracer is currently registered.
     *
     * @param tracer The tracer to register as the global tracer.
     * @return `true` if the tracer was successfully registered, or `false` if a tracer was already registered.
     */
    @JvmStatic
    fun registerIfAbsent(tracer: MotadataTracer): Boolean {
        return instance.compareAndSet(null, tracer)
    }

    /**
     * Retrieves the current active tracer for Motadata, or a no-operation tracer if none is active.
     *
     * @return The current instance of [MotadataTracer] if available. Otherwise, an instance of
     * [NoOpMotadataTracer] that performs no operations.
     */
    @JvmStatic
    fun get(): MotadataTracer = getOrNull() ?: NoOpMotadataTracer()

    /**
     * Retrieves the current instance of the MotadataTracer, if available.
     *
     * @return An instance of [MotadataTracer] or null.
     */
    fun getOrNull(): MotadataTracer? = instance.get()

    /**
     * Clears the current instance of the global Motadata tracer.
     *
     * This method sets the internal tracer instance to null, effectively
     * removing any active tracer currently held in the global state.
     * The general purpose is to use it for test implementation.
     */
    fun clear() {
        instance.set(null)
    }
}
