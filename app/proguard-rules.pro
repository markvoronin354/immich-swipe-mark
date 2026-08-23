# --- GSON RULES ---
# Prevent obfuscation of fields used by Gson
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep @SerializedName annotations
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# --- RETROFIT RULES ---
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# --- OKHTTP RULES ---
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
-dontwarn okio.**

# --- IMMICH SWIPE DATA MODELS ---
# We MUST keep these because Retrofit/Gson uses reflection to populate them.
# If they are obfuscated, keys won't match or methods won't be found.
-keep class com.markvoronin.immichswipe.domain.model.** { *; }
-keep class com.markvoronin.immichswipe.data.api.** { *; }

# --- ROOM RULES ---
-keep class * extends androidx.room.RoomDatabase
-keep class com.markvoronin.immichswipe.data.local.entity.** { *; }
-keep class com.markvoronin.immichswipe.data.local.dao.** { *; }
-keep class com.markvoronin.immichswipe.data.local.model.** { *; }

# --- COIL RULES ---
-keep class coil.** { *; }
-dontwarn coil.**

# --- MEDIA3 / EXOPLAYER ---
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
