/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.motadata.android.sessionreplay.internal.recorder.obfuscator

import android.os.Build
import com.motadata.android.lint.InternalApi

/**
 * Obfuscates string of text in session replay.
 *
 * DO NOT USE this class or its methods if you are not working on the internals of the Motadata SDK
 * or one of the cross platform frameworks.
 */
@InternalApi
interface StringObfuscator {

    /**
     * Obfuscates string of text in session replay.
     *
     * For Motadata internal use only.
     */
    @InternalApi
    fun obfuscate(stringValue: String): String

    companion object {
        internal const val CHARACTER_MASK = 'x'

        /**
         * Gets the instance of [StringObfuscator].
         *
         * For Motadata internal use only.
         */
        @InternalApi
        fun getStringObfuscator(): StringObfuscator {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                AndroidNStringObfuscator()
            } else {
                LegacyStringObfuscator()
            }
        }
    }
}
