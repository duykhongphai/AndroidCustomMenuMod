# Apktool compatibility

The profile-driven refactor does not prevent an Apktool-based integration for an APK you own or are authorized to modify. The UI remains programmatic Android Views, so most of the engine is ordinary compiled classes rather than layout resources.

## Important distinction

Apktool decodes and rebuilds APK resources and DEX bytecode represented as smali. It does not compile Java source and does not directly consume an Android AAR.

Therefore, building `menu-core-release.aar` is sufficient for a normal Gradle host, but not by itself for an Apktool-only workflow. The engine and the selected provider must already be compiled into code that the final APK can load.

## Final APK contract

For the engine to start, the final APK must contain all of the following:

- Compiled `com.nguyen.nebulamenu` engine classes.
- The host's compiled `MenuProvider` and `FeatureBridge` implementation.
- The `MenuOverlayService` declaration.
- Overlay, foreground-service, special-use, and notification permissions appropriate to the target Android version.
- Manifest metadata named `com.nguyen.nebulamenu.MENU_PROVIDER` whose value is the provider's exact class name.
- A host-controlled startup path that starts `MenuOverlayService` after the user has granted overlay access.

If one part is absent, the app may show the fallback profile, fail to display the overlay, or be stopped by Android.

## Why the engine remains portable

- It has no third-party runtime dependencies.
- Menu layouts are created programmatically.
- Engine and payload code do not reference host `R` values.
- Profile content is ordinary Java model construction.
- Provider selection is a single manifest metadata value.
- Resource names use the `nebula_` prefix to reduce collisions.

## Common limitations

- Rebuilding changes the APK signature. Apps that verify their own signature or package integrity may reject the rebuilt APK.
- DEX and smali merging must preserve class names and method references across multidex files.
- Resource identifiers can change during a rebuild and must be resolved by the build tool correctly.
- Android foreground-service and overlay policies vary by OS and target SDK.
- R8 keep rules from the AAR do not automatically help a manual Apktool workflow; the provider class name must remain exact.
- Vendor protections, anti-tamper systems, and online-service rules are outside the UI engine and are not bypassed by it.

For software you control, integrating the AAR at source level is more reliable than modifying the finished APK. Apktool should be reserved for authorized compatibility testing or environments where the original source build is unavailable.

## Standalone payload

`apktool-payload` provides a ready-to-compile compatibility payload. A validated build flow is:

1. Build `menu-core` and `apktool-payload` release AARs.
2. Compile both `classes.jar` files to one DEX with D8.
3. Disassemble that DEX with baksmali.
4. Place the resulting engine/payload smali in a new multidex folder.
5. Merge the permissions, provider metadata, permission activity, and service declarations.
6. Call `NebulaBootstrap.launch(Context)` from an authorized host startup point.
7. Rebuild, zipalign, and sign with an appropriate test or release key.

This workflow only supplies the overlay UI. It does not add host-specific feature behavior.
