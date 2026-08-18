# JNI resolves these methods by their Java class and method names.
-keep class com.nguyen.onyxmenu.nativebridge.NativeDemoRuntime {
    *;
}

-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}
