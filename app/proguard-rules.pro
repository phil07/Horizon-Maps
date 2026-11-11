# The app's own classes keep their names and members (settings, preferences and the saved data are read by name).
-keep class com.headunit.launcher.** { *; }
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable
-dontwarn org.slf4j.**
