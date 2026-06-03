/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.motadata.android.sessionreplay.internal.recorder

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.view.Window
import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import com.motadata.android.api.InternalLogger
import com.motadata.android.api.feature.FeatureSdkCore
import com.motadata.android.internal.time.TimeProvider
import com.motadata.android.sessionreplay.ImagePrivacy
import com.motadata.android.sessionreplay.MapperTypeWrapper
import com.motadata.android.sessionreplay.SessionReplayInternalCallback
import com.motadata.android.sessionreplay.TextAndInputPrivacy
import com.motadata.android.sessionreplay.internal.LifecycleCallback
import com.motadata.android.sessionreplay.internal.SessionReplayLifecycleCallback
import com.motadata.android.sessionreplay.internal.TouchPrivacyManager
import com.motadata.android.sessionreplay.internal.async.RecordedDataQueueHandler
import com.motadata.android.sessionreplay.internal.processor.MutationResolver
import com.motadata.android.sessionreplay.internal.processor.RecordedDataProcessor
import com.motadata.android.sessionreplay.internal.processor.ResourceQueueImpl
import com.motadata.android.sessionreplay.internal.processor.RumContextDataHandler
import com.motadata.android.sessionreplay.internal.recorder.callback.OnWindowRefreshedCallback
import com.motadata.android.sessionreplay.internal.recorder.mapper.DecorViewMapper
import com.motadata.android.sessionreplay.internal.recorder.mapper.HiddenViewMapper
import com.motadata.android.sessionreplay.internal.recorder.mapper.ViewWireframeMapper
import com.motadata.android.sessionreplay.internal.recorder.resources.BitmapCachesManager
import com.motadata.android.sessionreplay.internal.recorder.resources.BitmapPool
import com.motadata.android.sessionreplay.internal.recorder.resources.DefaultImageWireframeHelper
import com.motadata.android.sessionreplay.internal.recorder.resources.ImageTypeResolver
import com.motadata.android.sessionreplay.internal.recorder.resources.MD5HashGenerator
import com.motadata.android.sessionreplay.internal.recorder.resources.ResourceDrawableKeyGenerator
import com.motadata.android.sessionreplay.internal.recorder.resources.ResourceResolver
import com.motadata.android.sessionreplay.internal.recorder.resources.ResourcesLRUCache
import com.motadata.android.sessionreplay.internal.recorder.resources.WebPImageCompression
import com.motadata.android.sessionreplay.internal.resources.ResourceDataStoreManager
import com.motadata.android.sessionreplay.internal.storage.RecordWriter
import com.motadata.android.sessionreplay.internal.storage.ResourcesWriter
import com.motadata.android.sessionreplay.internal.utils.DrawableUtils
import com.motadata.android.sessionreplay.internal.utils.PathUtils
import com.motadata.android.sessionreplay.internal.utils.RumContextProvider
import com.motadata.android.sessionreplay.recorder.OptionSelectorDetector
import com.motadata.android.sessionreplay.utils.ColorStringFormatter
import com.motadata.android.sessionreplay.utils.DefaultColorStringFormatter
import com.motadata.android.sessionreplay.utils.DefaultViewBoundsResolver
import com.motadata.android.sessionreplay.utils.DefaultViewIdentifierResolver
import com.motadata.android.sessionreplay.utils.DrawableToColorMapper
import com.motadata.android.sessionreplay.utils.ViewBoundsResolver
import com.motadata.android.sessionreplay.utils.ViewIdentifierResolver
import java.util.concurrent.ConcurrentLinkedQueue

internal class SessionReplayRecorder : OnWindowRefreshedCallback, Recorder {

    private val appContext: Application
    private val textAndInputPrivacy: TextAndInputPrivacy
    private val imagePrivacy: ImagePrivacy
    private val customOptionSelectorDetectors: List<OptionSelectorDetector>
    private val windowInspector: WindowInspector
    private val windowCallbackInterceptor: WindowCallbackInterceptor
    private val sessionReplayLifecycleCallback: LifecycleCallback
    private val recordedDataQueueHandler: RecordedDataQueueHandler
    private val viewOnDrawInterceptor: ViewOnDrawInterceptor
    private val internalLogger: InternalLogger
    private val uiHandler: Handler
    private val resourceResolver: ResourceResolver
    private var shouldRecord = false

    @Suppress("LongParameterList")
    constructor(
        appContext: Application,
        resourcesWriter: ResourcesWriter,
        rumContextProvider: RumContextProvider,
        textAndInputPrivacy: TextAndInputPrivacy,
        imagePrivacy: ImagePrivacy,
        touchPrivacyManager: TouchPrivacyManager,
        recordWriter: RecordWriter,
        timeProvider: TimeProvider,
        mappers: List<MapperTypeWrapper<*>> = emptyList(),
        customOptionSelectorDetectors: List<OptionSelectorDetector> = emptyList(),
        customDrawableMappers: List<DrawableToColorMapper>,
        windowInspector: WindowInspector = WindowInspector,
        sdkCore: FeatureSdkCore,
        resourceDataStoreManager: ResourceDataStoreManager,
        dynamicOptimizationEnabled: Boolean,
        internalCallback: SessionReplayInternalCallback
    ) {
        val internalLogger = sdkCore.internalLogger
        val rumContextDataHandler = RumContextDataHandler(
            rumContextProvider,
            timeProvider,
            internalLogger
        )

        val processor = RecordedDataProcessor(
            resourceDataStoreManager,
            resourcesWriter,
            recordWriter,
            MutationResolver(internalLogger),
            timeProvider
        )

        this.appContext = appContext
        this.textAndInputPrivacy = textAndInputPrivacy
        this.imagePrivacy = imagePrivacy
        this.customOptionSelectorDetectors = customOptionSelectorDetectors
        this.windowInspector = windowInspector
        this.recordedDataQueueHandler = RecordedDataQueueHandler(
            processor = processor,
            rumContextDataHandler = rumContextDataHandler,
            internalLogger = internalLogger,
            executorService = sdkCore.createSingleThreadExecutorService(
                "sr-event-processing"
            ),
            recordedDataQueue = ConcurrentLinkedQueue(),
            timeProvider = timeProvider
        )

        val viewIdentifierResolver: ViewIdentifierResolver = DefaultViewIdentifierResolver
        val colorStringFormatter: ColorStringFormatter = DefaultColorStringFormatter
        val viewBoundsResolver: ViewBoundsResolver = DefaultViewBoundsResolver
        val drawableToColorMapper: DrawableToColorMapper =
            DrawableToColorMapper.getDefault(customDrawableMappers)

        val defaultVWM = ViewWireframeMapper(
            viewIdentifierResolver,
            colorStringFormatter,
            viewBoundsResolver,
            drawableToColorMapper
        )

        val bitmapCachesManager = BitmapCachesManager(
            bitmapPool = BitmapPool(),
            resourcesLRUCache = ResourcesLRUCache(),
            logger = internalLogger,
            keyGenerator = ResourceDrawableKeyGenerator()
        )

        this.resourceResolver = ResourceResolver(
            applicationContext = appContext,
            recordedDataQueueHandler = recordedDataQueueHandler,
            pathUtils = PathUtils(internalLogger, bitmapCachesManager),
            bitmapCachesManager = bitmapCachesManager,
            drawableUtils = DrawableUtils(
                internalLogger,
                bitmapCachesManager,
                sdkCore.createSingleThreadExecutorService("drawables")
            ),
            logger = internalLogger,
            md5HashGenerator = MD5HashGenerator(internalLogger),
            webPImageCompression = WebPImageCompression(internalLogger)
        )

        this.viewOnDrawInterceptor = ViewOnDrawInterceptor(
            internalLogger = internalLogger,
            onDrawListenerProducer = DefaultOnDrawListenerProducer(
                snapshotProducer = SnapshotProducer(
                    DefaultImageWireframeHelper(
                        logger = internalLogger,
                        resourceResolver = resourceResolver,
                        viewIdentifierResolver = viewIdentifierResolver,
                        viewUtilsInternal = ViewUtilsInternal(),
                        imageTypeResolver = ImageTypeResolver()
                    ),
                    TreeViewTraversal(
                        mappers = mappers,
                        defaultViewMapper = defaultVWM,
                        decorViewMapper = DecorViewMapper(defaultVWM, viewIdentifierResolver),
                        hiddenViewMapper = HiddenViewMapper(
                            viewBoundsResolver = viewBoundsResolver,
                            viewIdentifierResolver = viewIdentifierResolver
                        ),
                        viewUtilsInternal = ViewUtilsInternal(),
                        internalLogger = internalLogger
                    ),
                    ComposedOptionSelectorDetector(
                        customOptionSelectorDetectors + DefaultOptionSelectorDetector()
                    ),
                    touchPrivacyManager,
                    internalLogger = internalLogger
                ),
                recordedDataQueueHandler = recordedDataQueueHandler,
                sdkCore = sdkCore,
                dynamicOptimizationEnabled = dynamicOptimizationEnabled
            ),
            touchPrivacyManager = touchPrivacyManager
        )
        this.windowCallbackInterceptor = WindowCallbackInterceptor(
            recordedDataQueueHandler,
            viewOnDrawInterceptor,
            timeProvider,
            rumContextProvider,
            internalLogger,
            imagePrivacy,
            textAndInputPrivacy,
            touchPrivacyManager
        )
        this.sessionReplayLifecycleCallback = SessionReplayLifecycleCallback(this)

        // Register fragment lifecycle callbacks for clients initialized after the Application.onCreate phase
        internalCallback.getCurrentActivity()?.let {
            sessionReplayLifecycleCallback.setCurrentWindow(it)
            sessionReplayLifecycleCallback.registerFragmentLifecycleCallbacks(it)
        }

        // Expose this object so it can be used to dynamically add resources
        internalCallback.setResourceQueue(ResourceQueueImpl(this.recordedDataQueueHandler))

        this.uiHandler = Handler(Looper.getMainLooper())
        this.internalLogger = internalLogger
    }

    @VisibleForTesting
    @Suppress("LongParameterList")
    constructor(
        appContext: Application,
        textAndInputPrivacy: TextAndInputPrivacy,
        imagePrivacy: ImagePrivacy,
        customOptionSelectorDetectors: List<OptionSelectorDetector>,
        windowInspector: WindowInspector = WindowInspector,
        windowCallbackInterceptor: WindowCallbackInterceptor,
        sessionReplayLifecycleCallback: LifecycleCallback,
        viewOnDrawInterceptor: ViewOnDrawInterceptor,
        recordedDataQueueHandler: RecordedDataQueueHandler,
        resourceResolver: ResourceResolver,
        uiHandler: Handler,
        internalLogger: InternalLogger
    ) {
        this.appContext = appContext
        this.textAndInputPrivacy = textAndInputPrivacy
        this.imagePrivacy = imagePrivacy
        this.customOptionSelectorDetectors = customOptionSelectorDetectors
        this.windowInspector = windowInspector
        this.recordedDataQueueHandler = recordedDataQueueHandler
        this.viewOnDrawInterceptor = viewOnDrawInterceptor
        this.windowCallbackInterceptor = windowCallbackInterceptor
        this.sessionReplayLifecycleCallback = sessionReplayLifecycleCallback
        this.resourceResolver = resourceResolver
        this.uiHandler = uiHandler
        this.internalLogger = internalLogger
    }

    override fun stopProcessingRecords() {
        recordedDataQueueHandler.clearAndStopProcessingQueue()
    }

    override fun registerCallbacks() {
        appContext.registerActivityLifecycleCallbacks(sessionReplayLifecycleCallback)
        resourceResolver.registerCallbacks()
    }

    override fun unregisterCallbacks() {
        appContext.unregisterActivityLifecycleCallbacks(sessionReplayLifecycleCallback)
        resourceResolver.unregisterCallbacks()
    }

    override fun resumeRecorders() {
        uiHandler.post {
            shouldRecord = true
            val windows = sessionReplayLifecycleCallback.getCurrentWindows()
            val decorViews = windowInspector.getGlobalWindowViews(internalLogger)
            windowCallbackInterceptor.intercept(windows, appContext)
            viewOnDrawInterceptor.intercept(decorViews, textAndInputPrivacy, imagePrivacy)
        }
    }

    override fun stopRecorders() {
        uiHandler.post {
            viewOnDrawInterceptor.stopIntercepting()
            windowCallbackInterceptor.stopIntercepting()
            shouldRecord = false
        }
    }

    @MainThread
    override fun onWindowsAdded(windows: List<Window>) {
        if (shouldRecord) {
            val decorViews = windowInspector.getGlobalWindowViews(internalLogger)
            windowCallbackInterceptor.intercept(windows, appContext)
            viewOnDrawInterceptor.intercept(decorViews, textAndInputPrivacy, imagePrivacy)
        }
    }

    @MainThread
    override fun onWindowsRemoved(windows: List<Window>) {
        if (shouldRecord) {
            val decorViews = windowInspector.getGlobalWindowViews(internalLogger)
            windowCallbackInterceptor.stopIntercepting(windows)
            viewOnDrawInterceptor.intercept(decorViews, textAndInputPrivacy, imagePrivacy)
        }
    }
}
