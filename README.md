# Motadata RUM SDK for Android

Real User Monitoring (RUM) for Android and Android TV apps. Capture views, user actions,
network requests, errors, crashes, and performance — and send them to your Motadata platform.

## Installation

Available on Maven Central:

```kotlin
dependencies {
    implementation("com.motadata:motadata-rum-android:1.0.1")
    // optional — network resource tracking via OkHttp
    implementation("com.motadata:motadata-rum-android-okhttp:1.0.1")
}
```

## Quick start

Initialize the SDK once in your `Application.onCreate()`, then enable RUM with your Motadata endpoint:

```kotlin
val configuration = Configuration.Builder(clientToken, env, variant).build()
Motadata.initialize(this, configuration, TrackingConsent.GRANTED)

val rumConfiguration = RumConfiguration.Builder(applicationId)
    .useCustomEndpoint("https://<your-motadata-endpoint>/api/v2/rum/")
    .trackUserInteractions()
    .trackLongTasks()
    .useViewTrackingStrategy(ActivityViewTrackingStrategy(false))
    .build()
Rum.enable(rumConfiguration)
```

Get your **RUM application ID** and **client token** from your Motadata organization.

## Requirements

- Android `minSdk` 23+ (Android 6.0), `compileSdk` 36, Java 17

## License

Apache License 2.0 — see [LICENSE](LICENSE).
