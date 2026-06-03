/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */
package com.motadata.android.trace.internal

import com.motadata.android.trace.api.trace.MotadataTraceId
import com.motadata.android.utils.forge.Configurator
import com.datadog.trace.api.DDTraceId
import fr.xgouchet.elmyr.annotation.IntForgery
import fr.xgouchet.elmyr.annotation.LongForgery
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness

@Extensions(
    ExtendWith(MockitoExtension::class),
    ExtendWith(ForgeExtension::class)
)
@MockitoSettings(strictness = Strictness.LENIENT)
@ForgeConfiguration(Configurator::class)
class MotadataTraceIdAdapterTest {

    private lateinit var testedAdapter: MotadataTraceIdAdapter

    @Mock
    lateinit var mockDDTraceId: DDTraceId

    @IntForgery
    private var fakeInt = 0

    @LongForgery(min = 0)
    private var fakeLong = 0L

    @BeforeEach
    fun `set up`() {
        testedAdapter = MotadataTraceIdAdapter(mockDDTraceId)
        whenever(mockDDTraceId.toHexString()).thenReturn(fakeInt.toString(16))
        whenever(mockDDTraceId.toHexStringPadded(any())).thenReturn(fakeInt.toString(16))
        whenever(mockDDTraceId.toString()).thenReturn(fakeInt.toString())
    }

    @Test
    fun `M delegate to DDTraceId#toLong W toLong is called`() {
        // When
        testedAdapter.toLong()

        // Then
        verify(mockDDTraceId).toLong()
    }

    @Test
    fun `M delegate to DDTraceId#toString W toString is called`() {
        // When
        val actual = testedAdapter.toString()

        // Then
        assertThat(actual).isEqualTo(fakeInt.toString())
    }

    @Test
    fun `M delegate to DDTraceId#toHexString W toHexString is called`() {
        // When
        testedAdapter.toHexString()

        // Then
        verify(mockDDTraceId).toHexString()
    }

    @Test
    fun `M delegate to DDTraceId#toHighOrderLong W toHighOrderLong is called`() {
        // When
        testedAdapter.toHighOrderLong()

        // Then
        verify(mockDDTraceId).toHighOrderLong()
    }

    @Test
    fun `M delegate to DDTraceId#toHexStringPadded W toHexStringPadded is called`() {
        // When
        testedAdapter.toHexStringPadded(fakeInt)

        // Then
        verify(mockDDTraceId).toHexStringPadded(fakeInt)
    }

    @Test
    fun `M return expected W fromHex(String)`() {
        // Given
        val stringTraceId = fakeLong.toString()
        val expected = MotadataTraceIdAdapter(DDTraceId.fromHex(stringTraceId))

        // When
        val actual = MotadataTraceId.fromHex(stringTraceId)

        // Then
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `M return expected W toHexString()`() {
        // Given
        val ddTraceId = DDTraceId.from(fakeLong)

        // When
        val actual = MotadataTraceIdAdapter(ddTraceId).toHexString()

        // Then
        assertThat(actual).isEqualTo(ddTraceId.toHexString())
    }

    @Test
    fun `M return expected W toLong()`() {
        // Given
        val traceId = MotadataTraceIdAdapter(DDTraceId.from(fakeLong))

        // When
        val actual = traceId.toLong()

        // Then
        assertThat(actual).isEqualTo(fakeLong)
    }
}
