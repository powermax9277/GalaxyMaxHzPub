-keepattributes Signature

-dontwarn sun.misc.**

-optimizationpasses 60
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** e(...);
}
-allowaccessmodification
-flattenpackagehierarchy

