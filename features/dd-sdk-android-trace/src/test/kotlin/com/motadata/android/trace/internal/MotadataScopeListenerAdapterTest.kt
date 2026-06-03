/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */
package com.motadata.android.trace.internal

import com.motadata.android.trace.api.scope.MotadataScopeListener
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify

@Extensions(
    ExtendWith(MockitoExtension::class)
)
class MotadataScopeListenerAdapterTest {

    @Mock
    lateinit var delegate: MotadataScopeListener

    @Test
    fun `M delegate afterScopeClosed W afterScopeClosed is called`() {
        // Given
        val adapter = MotadataScopeListenerAdapter(delegate)

        // When
        adapter.afterScopeClosed()

        // Then
        verify(delegate).afterScopeClosed()
    }

    @Test
    fun `M delegate afterScopeActivated W afterScopeActivated is called`() {
        // Then
        val adapter = MotadataScopeListenerAdapter(delegate)

        // When
        adapter.afterScopeActivated()

        // Then
        verify(delegate).afterScopeActivated()
    }
}
