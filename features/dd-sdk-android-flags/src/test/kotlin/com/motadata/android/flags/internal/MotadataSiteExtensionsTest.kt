/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.motadata.android.flags.internal

import com.motadata.android.MotadataSite
import fr.xgouchet.elmyr.annotation.StringForgery
import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource

@ExtendWith(ForgeExtension::class)
internal class MotadataSiteExtensionsTest {

    // region getFlagsEndpoint - With Custom Domain

    @ParameterizedTest
    @MethodSource("supportedSitesWithCustomDomain")
    fun `M build flags endpoint W getFlagsEndpoint() { supported sites with custom domain }`(
        site: MotadataSite,
        expectedHostSuffix: String,
        @StringForgery customerDomain: String
    ) {
        // When
        val result = site.getFlagsEndpoint(customerDomain)

        // Then
        assertThat(result).isEqualTo("https://$customerDomain.$expectedHostSuffix/precompute-assignments")
    }

    // endregion

    // region getFlagsEndpoint - With Default Domain

    @ParameterizedTest
    @MethodSource("supportedSitesWithDefaultDomain")
    fun `M build flags endpoint W getFlagsEndpoint() { supported sites with preview domain }`(
        site: MotadataSite,
        expectedHost: String
    ) {
        // When
        val result = site.getFlagsEndpoint("preview")

        // Then
        assertThat(result).isEqualTo("https://$expectedHost/precompute-assignments")
    }

    // endregion

    // region getFlagsEndpoint - Error Cases

    @ParameterizedTest
    @EnumSource(MotadataSite::class, names = ["US1_FED", "US2_FED"])
    fun `M return null W getFlagsEndpoint() { unsupported site }`(
        site: MotadataSite,
        @StringForgery customerDomain: String
    ) {
        // When
        val result = site.getFlagsEndpoint(customerDomain)

        // Then
        assertThat(result).isNull()
    }

    // endregion

    // region getFlagsEndpoint - Edge Cases

    @ParameterizedTest
    @MethodSource("edgeCaseCustomerDomains")
    fun `M handle edge case customer domains W getFlagsEndpoint() { various edge cases }`(
        site: MotadataSite,
        customerDomain: String,
        expectedHost: String
    ) {
        // When
        val result = site.getFlagsEndpoint(customerDomain)

        // Then
        assertThat(result).isEqualTo("https://$expectedHost/precompute-assignments")
    }

    // endregion

    companion object {
        @Suppress("unused")
        @JvmStatic
        fun supportedSitesWithCustomDomain(): List<Arguments> = listOf(
            Arguments.of(MotadataSite.US1, "ff-cdn.datadoghq.com"),
            Arguments.of(MotadataSite.US3, "ff-cdn.us3.datadoghq.com"),
            Arguments.of(MotadataSite.US5, "ff-cdn.us5.datadoghq.com"),
            Arguments.of(MotadataSite.AP1, "ff-cdn.ap1.datadoghq.com"),
            Arguments.of(MotadataSite.AP2, "ff-cdn.ap2.datadoghq.com"),
            Arguments.of(MotadataSite.EU1, "ff-cdn.datadoghq.eu"),
            Arguments.of(MotadataSite.STAGING, "ff-cdn.datad0g.com")
        )

        @Suppress("unused")
        @JvmStatic
        fun supportedSitesWithDefaultDomain(): List<Arguments> = listOf(
            Arguments.of(MotadataSite.US1, "preview.ff-cdn.datadoghq.com"),
            Arguments.of(MotadataSite.EU1, "preview.ff-cdn.datadoghq.eu"),
            Arguments.of(MotadataSite.STAGING, "preview.ff-cdn.datad0g.com")
        )

        @Suppress("unused")
        @JvmStatic
        fun edgeCaseCustomerDomains(): List<Arguments> = listOf(
            // Domain with hyphens and underscores (special characters)
            Arguments.of(MotadataSite.US1, "test-domain_123", "test-domain_123.ff-cdn.datadoghq.com"),
            // Numeric-only domain
            Arguments.of(MotadataSite.US3, "12345", "12345.ff-cdn.us3.datadoghq.com"),
            // Domain with dots (subdomain-like)
            Arguments.of(MotadataSite.EU1, "my.customer.domain", "my.customer.domain.ff-cdn.datadoghq.eu")
        )
    }
}
