# Add project specific ProGuard rules here.
-keepattributes Signature

## For using GSON @Expose annotation
-keepattributes *Annotation*

# Gson specific classes
#-dontwarn sun.misc.**
-keep class com.google.gson.stream.** { *; }

# Prevent proguard from stripping interface information from TypeAdapter, TypeAdapterFactory,
# JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keep class com.tribalfs.gmh.BuildConfig { *; }
# Prevent R8 from leaving Data object members always null
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}
# Application classes that will be serialized/deserialized over Gson
-keep class com.tribalfs.gmh.profiles.ProfilesObj { *; }
-keep class com.tribalfs.gmh.profiles.ResolutionDetails { *; }
-keep class com.tribalfs.gmh.BuildConfig { *; }

-optimizationpasses 70
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** e(...);
}
#-overloadaggressively
-allowaccessmodification
-flattenpackagehierarchy
-printmapping mapping.txt


