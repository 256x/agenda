# Preserve stack trace line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keepclasseswithmembernames class * {
    @dagger.hilt.* <methods>;
    @javax.inject.* <fields>;
}

# ViewModel (prevent obfuscation of classes used via reflection)
-keep class * extends androidx.lifecycle.ViewModel { *; }

# App data classes used by file parsing / JSON
-keep class fumi.day.literalagenda.data.** { *; }

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
