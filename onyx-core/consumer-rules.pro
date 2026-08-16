# MenuProvider implementations are selected by class name from manifest metadata.
-keep class * implements com.nguyen.onyxmenu.engine.MenuProvider {
    public <init>();
    *;
}
