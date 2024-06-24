package hu.montlikadani.tablist.tablist;

import hu.montlikadani.tablist.TabList;
import hu.montlikadani.tablist.config.constantsLoader.TabConfigValues;
import hu.montlikadani.tablist.user.TabListUser;
import hu.montlikadani.tablist.utils.Util;
import java.util.logging.Level;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public final class TabToggleBase {

    public static final java.util.Set<UUID> TEMPORAL_PLAYER_CACHE = new java.util.HashSet<>();

    /**
     * Holds the information that the tablist is globally (for every online player) disabled or not.
     */
    public static boolean globallySwitched = false;

    /**
     * Checks if tablist is disabled globally (for everyone) or for the specified user.
     *
     * @param user the target {@link TabListUser} to check if the tablist is disabled
     * @return true if disabled for everyone or for the specific user, otherwise false.
     * @throws NullPointerException if user is null
     */
    public static boolean isDisabled(TabListUser user) {
        return globallySwitched || !user.isTabVisible();
    }

    TabToggleBase() {
    }

    public void load(TabList tl) {
        if (!TabConfigValues.isRememberToggledTablistToFile()) {
            return;
        }

        File file = new File(tl.getDataFolder(), "toggledtablists.yml");
        if (!file.exists()) {
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        if (!(globallySwitched = config.getBoolean("globallySwitched", false))) {
            org.bukkit.configuration.ConfigurationSection section = config.getConfigurationSection("tablists");

            if (section == null) {
                return;
            }

            for (String uuid : section.getKeys(false)) {
                if (section.getBoolean(uuid, false)) {
                    continue;
                }

                try {
                    UUID id = UUID.fromString(uuid);
                    java.util.Optional<TabListUser> user = tl.getUser(id);

                    if (user.isPresent()) {
                        user.get().setTabVisibility(false);
                    } else {
                        TEMPORAL_PLAYER_CACHE.add(id);
                    }
                } catch (IllegalArgumentException ignore) {
                }
            }

            return;
        }

        config.set("tablists", null);

        try {
            config.save(file);
        } catch (IOException ex) {
            Util.printTrace(Level.SEVERE, tl, ex.getMessage(), ex);
        }
    }

    public void save(TabList tl) {
        File file = new File(tl.getDataFolder(), "toggledtablists.yml");

        if (!TabConfigValues.isRememberToggledTablistToFile() || (!globallySwitched && tl.getUsers().stream().allMatch(TabListUser::isTabVisible))) {
            if (file.exists() && !file.delete()) {
                throw new RuntimeException("Failed to delete file toggledtablists.yml");
            }

            return;
        }

        if (!file.exists()) {
            try {
                if (!file.createNewFile()) {
                    throw new RuntimeException("Failed to create toggledtablists.yml file");
                }
            } catch (IOException ex) {
                Util.printTrace(Level.SEVERE, tl, ex.getMessage(), ex);
            }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set("tablists", null);

        if (globallySwitched) {
            config.set("globallySwitched", true);
        } else {
            for (TabListUser user : tl.getUsers()) {
                if (!user.isTabVisible()) {
                    config.set("tablists." + user.getUniqueId().toString(), false);
                }
            }

            for (UUID id : TEMPORAL_PLAYER_CACHE) {
                config.set("tablists." + id.toString(), false);
            }

            config.set("globallySwitched", null);
        }

        TEMPORAL_PLAYER_CACHE.clear();

        try {
            config.save(file);
        } catch (IOException ex) {
            Util.printTrace(Level.SEVERE, tl, ex.getMessage(), ex);
        }
    }
}
