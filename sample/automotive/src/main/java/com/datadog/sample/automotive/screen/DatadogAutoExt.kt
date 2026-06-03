/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.sample.automotive.screen

import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.OnClickListener
import com.motadata.android.Motadata
import com.motadata.android.api.SdkCore
import com.motadata.android.rum.GlobalRumMonitor
import com.motadata.android.rum.RumActionType

internal fun Screen.monitorGetTemplate(
    sdkCore: SdkCore = Motadata.getInstance()
) {
    GlobalRumMonitor.get(sdkCore).startView(
        key = javaClass.name,
        name = javaClass.simpleName
    )
}

internal fun Action.Builder.setMonitoredClickListener(
    sdkCore: SdkCore = Motadata.getInstance(),
    listener: OnClickListener
): Action.Builder {
    val builtAction = build()
    return setOnClickListener {
        GlobalRumMonitor.get(sdkCore).addAction(
            type = RumActionType.TAP,
            name = builtAction.title?.toString() ?: builtAction.icon.toString()
        )
        listener.onClick()
    }
}
