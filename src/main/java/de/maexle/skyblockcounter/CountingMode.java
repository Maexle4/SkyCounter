package de.maexle.skyblockcounter;

/**
 * Where the kill counter reads its number from.
 *
 * <p>The four modes differ in two ways: whether the value comes from Hypixel or is counted
 * locally, and whether a local count survives between game sessions.
 */
public enum CountingMode {

    /** Reads the Bestiary total from the Hypixel API. Nothing is stored locally. */
    API("api"),

    /**
     * Local counter persisted across restarts. The value keeps growing until it is reset
     * manually, so it behaves like a long-term personal record.
     */
    LOCAL_PERSISTENT("local_persistent"),

    /** Local counter that lives only as long as this client is running. */
    LOCAL_SESSION("local_session"),

    /** Local counter that is also cleared whenever the player changes world/island. */
    LOCAL_PER_WORLD("local_per_world");

    private final String id;

    CountingMode(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    /** True when the mode counts kills locally instead of asking Hypixel. */
    public boolean isLocal() {
        return this != API;
    }

    /** True when the count should be restored from the config on startup. */
    public boolean isPersistent() {
        return this == LOCAL_PERSISTENT;
    }

    /** True when changing world resets the counter. */
    public boolean resetsOnWorldChange() {
        return this == LOCAL_PER_WORLD;
    }

    public static CountingMode fromId(String id) {
        for (CountingMode mode : values()) {
            if (mode.id.equals(id)) {
                return mode;
            }
        }
        return API;
    }

    /** Localised display name, resolved through the mod language table. */
    public String displayName() {
        return de.maexle.skyblockcounter.i18n.ModLang.tr("mode." + id);
    }

    public String description() {
        return de.maexle.skyblockcounter.i18n.ModLang.tr("mode." + id + ".desc");
    }

    public CountingMode next() {
        CountingMode[] all = values();
        return all[(ordinal() + 1) % all.length];
    }
}
