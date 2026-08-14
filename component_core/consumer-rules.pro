# =============================================================================
# component_core Consumer ProGuard Rules
# 覆盖 core 内置网络 / Room / DataStore / Gson 等基础混淆需求；
# 业务 Entity、DTO 请在各 feature 模块 consumer-rules 中补充。
# =============================================================================

# ----- 通用反射 / 序列化 -----
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

# ----- Retrofit（官方推荐） -----
-dontwarn retrofit2.**
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# ----- OkHttp / Okio -----
-dontwarn okhttp3.**
-dontwarn okio.**

# ----- Gson -----
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ----- Room -----
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-dontwarn androidx.room.paging.**

# ----- DataStore -----
-keep class androidx.datastore.*.** { *; }
-dontwarn androidx.datastore.**

# ----- Kotlin 协程 -----
-dontwarn kotlinx.coroutines.**
