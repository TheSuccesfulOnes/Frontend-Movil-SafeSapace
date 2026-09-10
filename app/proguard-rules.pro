# Keep Retrofit request/response fields stable in minified builds.
# Gson uses field names for properties without @SerializedName. Obfuscating
# those fields would make release builds send or read invalid API contracts.
-keepclassmembers class com.experimentos.mobile.**.data.** {
    <fields>;
}
-keepattributes Signature
-keepattributes *Annotation*
