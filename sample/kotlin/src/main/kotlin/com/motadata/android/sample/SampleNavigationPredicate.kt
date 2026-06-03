/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.motadata.android.sample

import androidx.navigation.NavDestination
import com.motadata.android.rum.tracking.ComponentPredicate

internal class SampleNavigationPredicate : ComponentPredicate<NavDestination> {
    override fun accept(component: NavDestination): Boolean {
        return true
    }

    override fun getViewName(component: NavDestination): String {
        return component.label.toString()
    }
}
