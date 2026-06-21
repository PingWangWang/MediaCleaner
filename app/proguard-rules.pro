# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.RoomDatabase$Builder
-dontwarn androidx.room.paging.**

# Coil
-keep class coil.** { *; }
-dontwarn coil.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Gson/JSON
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.photocleaner.feature.appupdate.model.** { *; }

# Keep our model classes
-keep class com.photocleaner.core.common.model.** { *; }
-keep class com.photocleaner.core.database.entity.** { *; }

# Compose
-dontwarn androidx.compose.**
