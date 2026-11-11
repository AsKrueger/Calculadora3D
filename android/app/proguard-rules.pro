# kotlinx.serialization: conservar los serializadores generados de las clases @Serializable
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.tdcostmanager.app.**$$serializer { *; }
-keepclassmembers class com.tdcostmanager.app.** { *** Companion; }
-keepclasseswithmembers class com.tdcostmanager.app.** { kotlinx.serialization.KSerializer serializer(...); }
