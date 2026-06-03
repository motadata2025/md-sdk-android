/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */
package com.motadata.android.okhttp

import com.motadata.android.Motadata
import com.motadata.android.api.SdkCore
import com.motadata.android.api.feature.Feature
import com.motadata.android.core.stub.StubEvent
import com.motadata.android.core.stub.StubSDKCore
import com.motadata.android.okhttp.tests.assertj.SpansPayloadAssert
import com.motadata.android.okhttp.tests.elmyr.OkHttpConfigurator
import com.motadata.android.trace.MotadataTracing
import com.motadata.android.trace.GlobalMotadataTracer
import com.motadata.android.trace.Trace
import com.motadata.android.trace.TraceConfiguration
import com.motadata.android.trace.api.replace
import com.motadata.android.trace.api.span.MotadataSpan
import com.motadata.android.trace.internal._TraceInternalProxy
import com.motadata.android.trace.withinSpan
import com.datadog.tools.unit.extensions.TestConfigurationExtension
import com.datadog.tools.unit.getFieldValue
import com.datadog.tools.unit.getStaticValue
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness

@Extensions(
    ExtendWith(MockitoExtension::class),
    ExtendWith(ForgeExtension::class),
    ExtendWith(TestConfigurationExtension::class)
)
@ForgeConfiguration(OkHttpConfigurator::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SpanExtIntegrationTest {
    private lateinit var stubSdkCore: StubSDKCore
    private lateinit var mockServer: MockWebServer

    private val expectedEnv: String get() = stubSdkCore.getDatadogContext().env

    private val JsonObject.spanId: String
        get() = get("span_id").asString

    private val JsonObject.spans: JsonArray
        get() = getAsJsonArray("spans")

    private fun JsonArray.getObject(index: Int) = get(index).asJsonObject
    private fun StubEvent.asJson(): JsonObject = JsonParser.parseString(eventData).asJsonObject
    private fun MotadataSpan.getSpanId(): String = _TraceInternalProxy.spanIdConverter
        .toHexStringPadded(context().spanId)
    private fun registerTracer(
        sampleRate: Double? = null,
        partialFlushMinSpans: Int? = null
    ): Boolean {
        return GlobalMotadataTracer.replace(
            MotadataTracing.newTracerBuilder(stubSdkCore)
                .also {
                    if (sampleRate != null) it.withSampleRate(sampleRate)
                    if (partialFlushMinSpans != null) it.withPartialFlushMinSpans(partialFlushMinSpans)
                }
        )
    }

    @BeforeEach
    fun `set up`(forge: Forge) {
        stubSdkCore = StubSDKCore(forge)
        val registry: Any = Motadata::class.java.getStaticValue("registry")
        val instances: MutableMap<String, SdkCore> = registry.getFieldValue("instances")
        instances += stubSdkCore.name to stubSdkCore
        mockServer = MockWebServer()

        val fakeTraceConfiguration = TraceConfiguration.Builder().build()
        Trace.enable(fakeTraceConfiguration, stubSdkCore)

        mockServer.enqueue(MockResponse())
        mockServer.start()
    }

    @AfterEach
    fun `tear down`() {
        GlobalMotadataTracer.clear()
        Motadata.stopInstance(stubSdkCore.name)
        mockServer.shutdown()
    }

    @Test
    fun `M return expected events W withinSpan {two composite spans}`() {
        registerTracer(partialFlushMinSpans = 1)
        var isCalled = false
        var rootSpanId = ""
        var childSpanId = ""
        withinSpan("rootSpanOperation") {
            rootSpanId = getSpanId()
            withinSpan("childSpanOperation") {
                childSpanId = getSpanId()
                isCalled = true
            }
        }

        val eventsWritten = stubSdkCore.eventsWritten(Feature.TRACING_FEATURE_NAME)
        val rootSpanJson = eventsWritten[0].asJson()
        val childSpanJson = eventsWritten[1].asJson()

        assertThat(isCalled).isTrue
        SpansPayloadAssert.assertThat(rootSpanJson)
            .hasEnv(expectedEnv)
            .hasSpanAtIndexWith(0) {
                hasSamplingPriority(1)
                hasSpanId(rootSpanId)
                hasResource("rootSpanOperation")
                hasName("rootSpanOperation")
                hasType("custom")
                hasAgentPsr(1.0)
                hasSamplingPriority(1)
                hasValidMostSignificant64BitsTraceId()
                hasValidLeastSignificant64BitsTraceId()
            }

        SpansPayloadAssert.assertThat(childSpanJson)
            .hasEnv(expectedEnv)
            .hasSpanAtIndexWith(0) {
                hasSpanId(childSpanId)
                hasNoSamplingPriority()
                hasParentId(rootSpanId)
                hasResource("childSpanOperation")
                hasName("childSpanOperation")
                hasType("custom")
                hasSpanKind("client")
                hasNoAgentPsr()
                hasNoSamplingPriority()
                hasValidMostSignificant64BitsTraceId()
                hasValidLeastSignificant64BitsTraceId()
            }
    }

    @Test
    fun `M return expected events W withinSpan {three composite spans}`() {
        registerTracer(partialFlushMinSpans = 1)
        var isCalled = false
        var rootSpanId = ""
        var level1ChildSpanId = ""
        var level2ChildSpanId = ""
        withinSpan("rootSpanOperation") {
            rootSpanId = getSpanId()
            withinSpan("level1ChildSpanOperation") {
                level1ChildSpanId = getSpanId()
                withinSpan("level2ChildSpanOperation") {
                    isCalled = true
                    level2ChildSpanId = getSpanId()
                }
            }
        }

        val eventsWritten = stubSdkCore.eventsWritten(Feature.TRACING_FEATURE_NAME)
        val eventsJson = eventsWritten
            .map { it.asJson() }
            .associateBy { it.spans.getObject(0).spanId }

        val rootSpanJson = checkNotNull(eventsJson[rootSpanId])
        val child1Json = checkNotNull(eventsJson[level1ChildSpanId])
        val child2Json = checkNotNull(eventsJson[level2ChildSpanId])

        assertThat(isCalled).isTrue
        SpansPayloadAssert.assertThat(rootSpanJson)
            .hasEnv(expectedEnv)
            .hasSpanAtIndexWith(0) {
                hasSamplingPriority(1)
                hasSpanId(rootSpanId)
                hasResource("rootSpanOperation")
                hasName("rootSpanOperation")
                hasType("custom")
                hasAgentPsr(1.0)
                hasSamplingPriority(1)
                hasValidMostSignificant64BitsTraceId()
                hasValidLeastSignificant64BitsTraceId()
            }

        SpansPayloadAssert.assertThat(child1Json)
            .hasEnv(expectedEnv)
            .hasSpanAtIndexWith(0) {
                hasSpanId(level1ChildSpanId)
                hasNoSamplingPriority()
                hasParentId(rootSpanId)
                hasResource("level1ChildSpanOperation")
                hasName("level1ChildSpanOperation")
                hasType("custom")
                hasSpanKind("client")
                hasNoAgentPsr()
                hasNoSamplingPriority()
                hasValidMostSignificant64BitsTraceId()
                hasValidLeastSignificant64BitsTraceId()
            }

        SpansPayloadAssert.assertThat(child2Json)
            .hasEnv(expectedEnv)
            .hasSpanAtIndexWith(0) {
                hasSpanId(level2ChildSpanId)
                hasNoSamplingPriority()
                hasParentId(level1ChildSpanId)
                hasResource("level2ChildSpanOperation")
                hasName("level2ChildSpanOperation")
                hasType("custom")
                hasSpanKind("client")
                hasNoAgentPsr()
                hasNoSamplingPriority()
                hasValidMostSignificant64BitsTraceId()
                hasValidLeastSignificant64BitsTraceId()
            }
    }

    @Test
    fun `M return expected events W withinSpan {composite spans, explicit sample rate = 100}`() {
        registerTracer(sampleRate = 100.0, partialFlushMinSpans = 1)
        var isCalled = false
        var rootSpanId = ""
        var childSpanId = ""
        withinSpan("rootSpanOperation") {
            rootSpanId = getSpanId()
            withinSpan("childSpanOperation") {
                childSpanId = getSpanId()
                isCalled = true
            }
        }

        val eventsWritten = stubSdkCore.eventsWritten(Feature.TRACING_FEATURE_NAME)
        val rootSpanJson = eventsWritten[0].asJson()
        val childSpanJson = eventsWritten[1].asJson()

        assertThat(isCalled).isTrue
        SpansPayloadAssert.assertThat(rootSpanJson)
            .hasEnv(expectedEnv)
            .hasSpanAtIndexWith(0) {
                hasSamplingPriority(1)
                hasSpanId(rootSpanId)
                hasResource("rootSpanOperation")
                hasName("rootSpanOperation")
                hasType("custom")
                hasAgentPsr(1.0)
                hasSamplingPriority(1)
                hasValidMostSignificant64BitsTraceId()
                hasValidLeastSignificant64BitsTraceId()
            }

        SpansPayloadAssert.assertThat(childSpanJson)
            .hasEnv(expectedEnv)
            .hasSpanAtIndexWith(0) {
                hasSpanId(childSpanId)
                hasNoSamplingPriority()
                hasParentId(rootSpanId)
                hasResource("childSpanOperation")
                hasName("childSpanOperation")
                hasType("custom")
                hasSpanKind("client")
                hasNoAgentPsr()
                hasNoSamplingPriority()
                hasValidMostSignificant64BitsTraceId()
                hasValidLeastSignificant64BitsTraceId()
            }
    }
}
