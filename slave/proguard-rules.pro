# WebSocket
-keep class org.java_websocket.** { *; }
-dontwarn org.java_websocket.**

# Gson
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# 应用类
-keep class com.slave.remote.** { *; }
