# R8 / ProGuard rules for Airlock release builds.
#
# Strategy: let R8 shrink the app + AndroidX + Compose (default consumer rules handle those),
# but KEEP PDFBox-Android wholesale — it resolves fonts/filters by reflection and class name,
# so shrinking it risks runtime breakage on PDF operations. We verify with the instrumented
# PDF tests before trusting any release.

# --- PDFBox-Android (com.tom_roush) + bundled fontbox ---
-keep class com.tom_roush.** { *; }
-dontwarn com.tom_roush.**

# PDFBox references desktop Java APIs that don't exist on Android; silence those.
-dontwarn java.awt.**
-dontwarn javax.imageio.**
-dontwarn javax.xml.**
-dontwarn org.apache.**

# BouncyCastle is an optional transitive dep used for encrypted PDFs; keep if present.
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# --- Kotlin / coroutines metadata (safe, standard) ---
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-dontwarn kotlinx.coroutines.**

# Airlock's own code uses no reflection; keeping it is cheap (tiny codebase) and lets the
# instrumented tests link against it when validating R8.
-keep class com.airlock.** { *; }

# androidx.test references build-time annotations not present at runtime. Only relevant when the
# instrumentation-test APK is itself minified (our R8 verification path); harmless otherwise.
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn javax.lang.model.**
-dontwarn org.checkerframework.**
-dontwarn com.google.auto.value.**
