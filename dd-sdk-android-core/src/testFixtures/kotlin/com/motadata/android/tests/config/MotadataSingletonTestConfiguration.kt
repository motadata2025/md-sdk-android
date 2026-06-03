/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.motadata.android.tests.config

import com.motadata.android.Motadata
import com.motadata.android.core.InternalSdkCore
import com.datadog.tools.unit.extensions.config.MockTestConfiguration
import fr.xgouchet.elmyr.Forge

class MotadataSingletonTestConfiguration :
    MockTestConfiguration<InternalSdkCore>(InternalSdkCore::class.java) {

    override fun setUp(forge: Forge) {
        super.setUp(forge)

        Motadata.registry.register(null, mockInstance)
    }

    override fun tearDown(forge: Forge) {
        clearRegistry()
        super.tearDown(forge)
    }

    fun clearRegistry() {
        Motadata.registry.clear()
    }
}
