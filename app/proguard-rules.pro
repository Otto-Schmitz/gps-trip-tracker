# Room generated code is safe; keep entity constructors used via reflection.
-keep class com.gearhead.redline.data.local.entity.** { *; }

# Keep Compose runtime metadata (defaults are usually enough; explicit for safety).
-keepclassmembers class **$$serializer { *; }
