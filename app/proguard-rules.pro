# Keep protobuf lite
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.protobuf.**

# Protobuf generated messages (app/src/main/proto) are parsed via GeneratedMessageLite
# and rely on field names like `xxx_` at runtime; R8 obfuscation of these fields breaks parsing.
-keep class blbl.cat3399.proto.** { *; }

# Keep IjkPlayer Java API names (JNI depends on stable class/method/field names).
# Without this, release (R8) obfuscation can make `libijkplayer.so` fail to bind and crash on startup.
-keep class tv.danmaku.ijk.** { *; }
-keep class com.debugly.ijkplayer.** { *; }

# Conscrypt: suppress R8 warnings about platform-internal classes that are only present
# on specific Android versions (KitKat / pre-KitKat) and are referenced reflectively.
-dontwarn com.android.org.conscrypt.SSLParametersImpl
-dontwarn org.apache.harmony.xnet.provider.jsse.SSLParametersImpl
-dontwarn org.conscrypt.**

# Conscrypt references platform-internal SSL classes that are not present in the compile classpath.
# These are only used on old Android versions at runtime; suppress R8 missing-class errors.
-dontwarn com.android.org.conscrypt.SSLParametersImpl
-dontwarn org.apache.harmony.xnet.provider.jsse.SSLParametersImpl
-dontwarn org.conscrypt.**
