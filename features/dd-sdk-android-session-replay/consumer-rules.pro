# Keep the optional selector class name. We need this in the SR recorder.
-keepnames class * extends android.view.View
-keepnames class * extends android.graphics.drawable.Drawable
-keepnames class * extends android.graphics.ColorFilter

# Kept for our internal telemetry
-keepnames class com.motadata.android.sessionreplay.internal.recorder.listener.WindowsOnDrawListener
-keepnames class com.motadata.android.sessionreplay.internal.recorder.TreeViewTraversal
-keepnames class * extends com.motadata.android.sessionreplay.recorder.mapper.WireframeMapper
-keepnames class * extends com.motadata.android.sessionreplay.internal.async.RecordedDataQueueItem

# Keep the fine grained masking level enums
-keepnames enum * extends com.motadata.android.sessionreplay.PrivacyLevel { *; }
