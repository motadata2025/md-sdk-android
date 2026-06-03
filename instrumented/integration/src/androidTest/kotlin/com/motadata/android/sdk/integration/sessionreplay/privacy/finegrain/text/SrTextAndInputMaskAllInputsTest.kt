/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.motadata.android.sdk.integration.sessionreplay.privacy.finegrain.text

import com.motadata.android.privacy.TrackingConsent
import com.motadata.android.sdk.integration.sessionreplay.SessionReplayTextAndInputPrivacyActivity
import com.motadata.android.sdk.rules.SessionReplayTestRule
import com.motadata.android.sdk.utils.SR_TEXT_AND_INPUT_PRIVACY
import com.motadata.android.sessionreplay.TextAndInputPrivacy
import org.junit.Rule
import org.junit.Test

internal class SrTextAndInputMaskAllInputsTest :
    TextAndInputPrivacyTestBase<SessionReplayTextAndInputPrivacyActivity>() {

    @get:Rule
    override val rule = SessionReplayTestRule(
        SessionReplayTextAndInputPrivacyActivity::class.java,
        trackingConsent = TrackingConsent.GRANTED,
        keepRequests = true,
        intentExtras = mapOf(
            SR_TEXT_AND_INPUT_PRIVACY to TextAndInputPrivacy.MASK_ALL_INPUTS
        )
    )

    @Test
    fun assessMaskAllInputsPayload() {
        assertStaticTextVisible(rule, "Default Text View")
        assertInputTextMaskedWithFixedMask(rule)
    }
}
