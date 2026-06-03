/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.motadata.android.rx

import com.motadata.android.api.SdkCore
import com.motadata.android.rum.GlobalRumMonitor
import com.motadata.android.rum.RumErrorSource
import com.motadata.android.rum.RumMonitor
import com.datadog.tools.unit.forge.BaseConfigurator
import fr.xgouchet.elmyr.annotation.Forgery
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.verify
import org.mockito.quality.Strictness
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.jvm.isAccessible

@Extensions(
    ExtendWith(
        MockitoExtension::class,
        ForgeExtension::class
    )
)
@ForgeConfiguration(value = BaseConfigurator::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MotadataRumErrorConsumerTest {

    lateinit var testedConsumer: MotadataRumErrorConsumer

    @Mock
    lateinit var mockRumMonitor: RumMonitor

    @Mock
    lateinit var mockSdkCore: SdkCore

    @Forgery
    lateinit var fakeException: Throwable

    @BeforeEach
    fun `set up`() {
        GlobalRumMonitor::class.declaredFunctions.first { it.name == "registerIfAbsent" }.apply {
            isAccessible = true
            call(GlobalRumMonitor::class.objectInstance, mockRumMonitor, mockSdkCore)
        }
        testedConsumer = MotadataRumErrorConsumer(mockSdkCore)
    }

    @AfterEach
    fun `tear down`() {
        GlobalRumMonitor::class.java.getDeclaredMethod("reset").apply {
            isAccessible = true
            invoke(null)
        }
    }

    @Test
    fun `M send an error event W intercepting an exception`() {
        // WHEN
        testedConsumer.accept(fakeException)

        // THEN
        verify(mockRumMonitor).addError(
            MotadataRumErrorConsumer.REQUEST_ERROR_MESSAGE,
            RumErrorSource.SOURCE,
            fakeException,
            emptyMap()
        )
    }
}
