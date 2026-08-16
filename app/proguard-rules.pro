# onyx-core supplies consumer rules for manifest-discovered MenuProvider classes.
# JNI resolves these methods by their Java class and method names.
-keep class com.nguyen.onyxmenu.demo.nativebridge.NativeDemoRuntime {
    *;
}

-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}
