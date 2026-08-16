# Giữ nguyên lớp và tên phương thức được JNI tra cứu theo quy ước tên.
-keep class com.nguyen.onyxpayload.nativebridge.MenuFreeFireRuntime {
    *;
}

-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}
