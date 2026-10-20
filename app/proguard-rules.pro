# KataDojo release rules.
# Room, DataStore, Navigation and Compose ship their own consumer rules.
# Enums are persisted by name (Room/DataStore), so keep their names stable.
-keepclassmembers enum com.katadojo.app.data.model.** {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
