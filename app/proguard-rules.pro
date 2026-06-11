# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes InnerClasses

# Gson - keep serializable classes
-keep class com.cznwiki.app.data.entity.** { *; }
-keep class com.cznwiki.app.network.RemoteUpdateManager$RemoteVersion { *; }

# Gson TypeToken
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

# Keep Gson classes
-keep class com.google.gson.** { *; }
-keep class com.google.gson.stream.** { *; }
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Room - keep generated DAO/DB implementations to prevent null-check optimization removal
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Dao class * { *; }
-dontwarn androidx.room.paging.**
