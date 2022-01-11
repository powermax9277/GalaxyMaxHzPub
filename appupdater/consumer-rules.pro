-keepattributes Signature

-dontwarn sun.misc.**

-optimizationpasses 50
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** e(...);
}
-allowaccessmodification
-flattenpackagehierarchy

