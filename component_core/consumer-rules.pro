# Retrofit / OkHttp / Gson 常用 keep 规则（Release 混淆占位，接入 minify 时生效）
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepattributes Signature
-keepattributes Exceptions
