-keep class com.motadata.android.trace.GlobalDatadogTracer {
    public com.motadata.android.trace.api.tracer.DatadogTracer getOrNull();
    public static com.motadata.android.trace.GlobalDatadogTracer INSTANCE;
}
-keepclassmembernames class org.jctools.** { *; }
