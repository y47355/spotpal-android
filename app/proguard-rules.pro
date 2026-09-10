# 搭趣 SpotPal R8 规则（详细设计 §4 安全：serialization 白名单）
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

# kotlinx-serialization
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.spotpal.core.model.**$$serializer { *; }
-keepclassmembers class com.spotpal.core.model.** { *** Companion; }
-keepclasseswithmembers class com.spotpal.core.model.** { kotlinx.serialization.KSerializer serializer(...); }

# Retrofit 接口
-keep interface com.spotpal.core.network.SpotPalApi { *; }
-keepattributes Signature, Exceptions
