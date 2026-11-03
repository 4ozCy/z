# The player service is started by name from the manifest and by Media3.
-keep class com.glassplayer.app.PlaybackService { *; }

# Annotation-only classes that Guava and Media3 mention but never load.
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.google.j2objc.annotations.**
-dontwarn org.checkerframework.**
-dontwarn org.codehaus.mojo.animal_sniffer.**
-dontwarn javax.annotation.**
-dontwarn afu.org.checkerframework.**
