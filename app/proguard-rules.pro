# Zmanim / JewishCalendar
-keep class com.kosherjava.** { *; }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.jqwave.data.**$$serializer { *; }
-keepclassmembers class com.jqwave.data.** {
    *** Companion;
}
