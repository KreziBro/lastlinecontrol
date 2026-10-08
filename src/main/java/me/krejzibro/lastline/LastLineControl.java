package me.krejzibro.lastline;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.HashSet;
import java.util.Scanner;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.GlowItemFrame;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LastLineControl extends JavaPlugin implements Listener, CommandExecutor {

    private final HashSet<UUID> activeGod = new HashSet<>();
    private final HashSet<UUID> activeFly = new HashSet<>();
    private String latestVersion = null;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        registerCommand("god");
        registerCommand("fly");
        registerCommand("llc");
        loadPersistence();
        if (getConfig().getBoolean("settings.check-updates", true)) {
            checkUpdates();
        } else {
            getLogger().info("Update checker is disabled in config.");
        }
    }

    @Override
    public void onDisable() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getAllowFlight()) activeFly.remove(p.getUniqueId());
        }
        savePersistenceConfig();
    }

    private void loadPersistence() {
        activeGod.clear();
        activeFly.clear();
        if (getConfig().isList("persistence.god")) {
            for (String s : getConfig().getStringList("persistence.god")) {
                try { activeGod.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
            }
        }
        if (getConfig().isList("persistence.fly")) {
            for (String s : getConfig().getStringList("persistence.fly")) {
                try { activeFly.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
            }
        }
        for (Player p : Bukkit.getOnlinePlayers()) restorePlayerState(p);
        getLogger().info("Loaded " + activeGod.size() + " god, " + activeFly.size() + " fly entries.");
    }

    private void savePersistenceConfig() {
        getConfig().set("persistence.god", activeGod.stream().map(UUID::toString).toList());
        getConfig().set("persistence.fly", activeFly.stream().map(UUID::toString).toList());
        saveConfig();
    }

    private void restorePlayerState(@NotNull Player player) {
        UUID uid = player.getUniqueId();
        if (activeGod.contains(uid)) player.setInvulnerable(true);
        if (activeFly.contains(uid)) {
            player.setAllowFlight(true);
            player.setFlying(true);
        }
    }

    private boolean checkRestriction(Player player, String actionKey) {
        if (player.hasPermission("lastline.bypass." + actionKey)) return false;
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return false;
        if (activeGod.contains(player.getUniqueId()) && getConfig().getBoolean("settings.disable-" + actionKey + "-in-god", true)) {
            player.sendActionBar(Component.text("\u00a7f\u0412\u044b \u043d\u0435 \u043c\u043e\u0436\u0435\u0442\u0435 \u0441\u0434\u0435\u043b\u0430\u0442\u044c \u044d\u0442\u043e \u0432 \u0440\u0435\u0436\u0438\u043c\u0435 \u00a76god"));
            return true;
        }
        if (activeFly.contains(player.getUniqueId()) && getConfig().getBoolean("settings.disable-" + actionKey + "-in-fly", true)) {
            player.sendActionBar(Component.text("\u00a7f\u0412\u044b \u043d\u0435 \u043c\u043e\u0436\u0435\u0442\u0435 \u0441\u0434\u0435\u043b\u0430\u0442\u044c \u044d\u0442\u043e \u0432 \u0440\u0435\u0436\u0438\u043c\u0435 \u00a76fly"));
            return true;
        }
        return false;
    }

    @EventHandler
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();
        restorePlayerState(player);
        if (player.hasPermission("lastline.admin")
                && getConfig().getBoolean("settings.check-updates", true)
                && latestVersion != null && !latestVersion.isEmpty()) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                player.sendMessage("\u00a76[LastLineControl] \u00a7f\u0414\u043e\u0441\u0442\u0443\u043f\u043d\u0430 \u043d\u043e\u0432\u0430\u044f \u0432\u0435\u0440\u0441\u0438\u044f: \u00a7a" + latestVersion);
                player.sendMessage("\u00a76[LastLineControl] \u00a7f\u0421\u043a\u0430\u0447\u0430\u0442\u044c: \u00a7bhttps://modrinth.com/plugin/GvX5Lmcy");
            }, 40L);
        }
    }

    @EventHandler
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        savePersistenceConfig();
    }

    @EventHandler
    public void onPickup(@NotNull EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (checkRestriction(player, "pickup")) event.setCancelled(true);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (checkRestriction(event.getPlayer(), "block-break")) event.setCancelled(true);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (checkRestriction(event.getPlayer(), "block-place")) event.setCancelled(true);
    }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent event) {
        if (checkRestriction(event.getPlayer(), "drop-item")) event.setCancelled(true);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (event.getEntity() instanceof ArmorStand
                || event.getEntity() instanceof ItemFrame
                || event.getEntity() instanceof GlowItemFrame) {
            if (checkRestriction(attacker, "interact-entities")) event.setCancelled(true);
        } else if (event.getEntity() instanceof Player) {
            if (checkRestriction(attacker, "pvp")) event.setCancelled(true);
        } else if (event.getEntity() instanceof LivingEntity) {
            if (checkRestriction(attacker, "hit-mob")) event.setCancelled(true);
        }
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player)) return;
        String type = event.getEntity().getType().name();
        if (type.equals("ENDER_PEARL") || type.equals("SPLASH_POTION")
                || type.equals("LINGERING_POTION") || type.equals("SNOWBALL")
                || type.equals("TRIDENT")) {
            if (checkRestriction(player, "projectiles")) event.setCancelled(true);
        }
    }

    @EventHandler
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (!(event.getEntered() instanceof Player player)) return;
        if (checkRestriction(player, "vehicles")) event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        InventoryType type = event.getInventory().getType();
        if (type == InventoryType.CHEST) {
            if (checkRestriction(player, "chest")) event.setCancelled(true);
        } else if (type == InventoryType.BARREL || type == InventoryType.FURNACE
                || type == InventoryType.DISPENSER || type == InventoryType.DROPPER
                || type == InventoryType.HOPPER || type == InventoryType.SMOKER
                || type == InventoryType.BLAST_FURNACE || type == InventoryType.BREWING) {
            if (checkRestriction(player, "containers")) event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) return;
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        Block block = event.getClickedBlock();
        if (item != null) {
            String mat = item.getType().name();
            if (mat.equals("BOW") || mat.equals("CROSSBOW")) {
                if (checkRestriction(player, "interact-bow")) event.setCancelled(true);
            } else if (mat.equals("END_CRYSTAL")) {
                if (checkRestriction(player, "interact-crystal")) event.setCancelled(true);
            } else if (mat.equals("FLINT_AND_STEEL")) {
                if (checkRestriction(player, "interact-flint")) event.setCancelled(true);
            } else if (mat.endsWith("_SPAWN_EGG")) {
                if (checkRestriction(player, "spawn-eggs")) event.setCancelled(true);
            }
        }
        if (block != null) {
            String mat = block.getType().name();
            if (mat.equals("ENDER_CHEST")) {
                if (checkRestriction(player, "interact-enderchest")) event.setCancelled(true);
            } else if (mat.endsWith("SHULKER_BOX")) {
                if (checkRestriction(player, "interact-shulker")) event.setCancelled(true);
            }
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        @Nullable String rawPrefix = getConfig().getString("messages.prefix");
        String prefix = (rawPrefix != null ? rawPrefix : "\u00a76[\u0421\u0435\u0440\u0432\u0435\u0440] ").replace("&", "\u00a7");

        if (cmd.getName().equalsIgnoreCase("llc")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("lastline.admin")) {
                    sender.sendMessage(prefix + "\u00a7c\u0423 \u0432\u0430\u0441 \u043d\u0435\u0442 \u043f\u0440\u0430\u0432!");
                    return true;
                }
                reloadConfig();
                loadPersistence();
                sender.sendMessage(prefix + "\u00a7a\u041a\u043e\u043d\u0444\u0438\u0433\u0443\u0440\u0430\u0446\u0438\u044f \u0443\u0441\u043f\u0435\u0448\u043d\u043e \u043f\u0435\u0440\u0435\u0437\u0430\u0433\u0440\u0443\u0436\u0435\u043d\u0430!");
                return true;
            }
            sender.sendMessage(prefix + "\u00a7f\u0418\u0441\u043f\u043e\u043b\u044c\u0437\u0443\u0439\u0442\u0435: \u00a76/llc reload");
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("god")) {
            if (args.length > 0) {
                if (!sender.hasPermission("lastline.admin")) {
                    sender.sendMessage(prefix + "\u00a7c\u0423 \u0432\u0430\u0441 \u043d\u0435\u0442 \u043f\u0440\u0430\u0432 \u0434\u043b\u044f \u0432\u044b\u0434\u0430\u0447\u0438 \u0440\u0435\u0436\u0438\u043c\u0430 \u0431\u043e\u0433\u0430 \u0434\u0440\u0443\u0433\u0438\u043c \u0438\u0433\u0440\u043e\u043a\u0430\u043c!");
                    return true;
                }
                Player target = resolveTarget(sender, args, prefix);
                if (target == null) return true;
                toggleGod(target, prefix);
                String sw = activeGod.contains(target.getUniqueId()) ? "\u00a7a\u0432\u043a\u043b\u044e\u0447\u0451\u043d" : "\u00a7c\u0432\u044b\u043a\u043b\u044e\u0447\u0435\u043d";
                sender.sendMessage(prefix + "\u00a7f\u0420\u0435\u0436\u0438\u043c \u0431\u043e\u0433\u0430 \u0434\u043b\u044f \u00a76" + target.getName() + " \u00a7f" + sw);
                return true;
            }
            if (!(sender instanceof Player player)) {
                sender.sendMessage(prefix + "\u00a7c\u041a\u043e\u043d\u0441\u043e\u043b\u044c \u0434\u043e\u043b\u0436\u043d\u0430 \u0443\u043a\u0430\u0437\u0430\u0442\u044c \u043d\u0438\u043a: \u00a7f/god <\u043d\u0438\u043a>");
                return true;
            }
            if (!player.hasPermission("lastline.god")) {
                player.sendMessage(prefix + "\u00a7c\u0423 \u0432\u0430\u0441 \u043d\u0435\u0442 \u043f\u0440\u0430\u0432 \u043d\u0430 \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u043d\u0438\u0435 \u044d\u0442\u043e\u0439 \u043a\u043e\u043c\u0430\u043d\u0434\u044b!");
                return true;
            }
            toggleGod(player, prefix);
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("fly")) {
            if (args.length > 0) {
                if (!sender.hasPermission("lastline.admin")) {
                    sender.sendMessage(prefix + "\u00a7c\u0423 \u0432\u0430\u0441 \u043d\u0435\u0442 \u043f\u0440\u0430\u0432 \u0434\u043b\u044f \u0432\u044b\u0434\u0430\u0447\u0438 \u043f\u043e\u043b\u0451\u0442\u0430 \u0434\u0440\u0443\u0433\u0438\u043c \u0438\u0433\u0440\u043e\u043a\u0430\u043c!");
                    return true;
                }
                Player target = resolveTarget(sender, args, prefix);
                if (target == null) return true;
                toggleFly(target, prefix);
                String sw = activeFly.contains(target.getUniqueId()) ? "\u00a7a\u0432\u043a\u043b\u044e\u0447\u0451\u043d" : "\u00a7c\u0432\u044b\u043a\u043b\u044e\u0447\u0435\u043d";
                sender.sendMessage(prefix + "\u00a7f\u0420\u0435\u0436\u0438\u043c \u043f\u043e\u043b\u0451\u0442\u0430 \u0434\u043b\u044f \u00a76" + target.getName() + " \u00a7f" + sw);
                return true;
            }
            if (!(sender instanceof Player player)) {
                sender.sendMessage(prefix + "\u00a7c\u041a\u043e\u043d\u0441\u043e\u043b\u044c \u0434\u043e\u043b\u0436\u043d\u0430 \u0443\u043a\u0430\u0437\u0430\u0442\u044c \u043d\u0438\u043a: \u00a7f/fly <\u043d\u0438\u043a>");
                return true;
            }
            if (!player.hasPermission("lastline.fly")) {
                player.sendMessage(prefix + "\u00a7c\u0423 \u0432\u0430\u0441 \u043d\u0435\u0442 \u043f\u0440\u0430\u0432 \u043d\u0430 \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u043d\u0438\u0435 \u044d\u0442\u043e\u0439 \u043a\u043e\u043c\u0430\u043d\u0434\u044b!");
                return true;
            }
            toggleFly(player, prefix);
            return true;
        }

        return true;
    }

    private void toggleGod(@NotNull Player player, @NotNull String prefix) {
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            player.sendMessage(prefix + "\u00a7c\u0412\u044b \u0432 \u043a\u0440\u0435\u0430\u0442\u0438\u0432\u0435/\u043d\u0430\u0431\u043b\u044e\u0434\u0430\u0442\u0435\u043b\u0435 \u0438 \u043d\u0435 \u043c\u043e\u0436\u0435\u0442\u0435 \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u044c \u0440\u0435\u0436\u0438\u043c \u0431\u043e\u0433\u0430!");
            return;
        }
        UUID uid = player.getUniqueId();
        if (activeGod.contains(uid)) {
            activeGod.remove(uid);
            player.setInvulnerable(false);
            player.sendMessage(prefix + "\u00a7f\u0420\u0435\u0436\u0438\u043c \u0431\u043e\u0433\u0430 \u00a7c\u0432\u044b\u043a\u043b\u044e\u0447\u0435\u043d");
        } else {
            activeGod.add(uid);
            player.setInvulnerable(true);
            player.sendMessage(prefix + "\u00a7f\u0420\u0435\u0436\u0438\u043c \u0431\u043e\u0433\u0430 \u00a7a\u0432\u043a\u043b\u044e\u0447\u0435\u043d");
        }
        savePersistenceConfig();
    }

    private void toggleFly(@NotNull Player player, @NotNull String prefix) {
        if (player.getGameMode() == GameMode.SPECTATOR) {
            player.sendMessage(prefix + "\u00a7c\u0412 \u0440\u0435\u0436\u0438\u043c\u0435 \u043d\u0430\u0431\u043b\u044e\u0434\u0430\u0442\u0435\u043b\u044f \u043f\u043e\u043b\u0451\u0442 \u043d\u0435\u043b\u044c\u0437\u044f \u0438\u0437\u043c\u0435\u043d\u0438\u0442\u044c!");
            return;
        }
        UUID uid = player.getUniqueId();
        if (activeFly.contains(uid)) {
            activeFly.remove(uid);
            player.setAllowFlight(false);
            if (player.isFlying()) player.setFlying(false);
            player.sendMessage(prefix + "\u00a7f\u0420\u0435\u0436\u0438\u043c \u043f\u043e\u043b\u0451\u0442\u0430 \u00a7c\u0432\u044b\u043a\u043b\u044e\u0447\u0435\u043d");
        } else {
            activeFly.add(uid);
            player.setAllowFlight(true);
            player.sendMessage(prefix + "\u00a7f\u0420\u0435\u0436\u0438\u043c \u043f\u043e\u043b\u0451\u0442\u0430 \u00a7a\u0432\u043a\u043b\u044e\u0447\u0435\u043d");
        }
        savePersistenceConfig();
    }

    private void checkUpdates() {
        String currentVersion = getDescription().getVersion();
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                URL url = new URI("https://api.modrinth.com/v2/project/GvX5Lmcy/version").toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", "LastLineControl/" + currentVersion);
                Scanner scanner = new Scanner(conn.getInputStream());
                String response = scanner.useDelimiter("\\A").next();
                scanner.close();
                String latest = response.split("\"version_number\":\"")[1].split("\"")[0];
                if (isNewerVersion(latest, currentVersion)) {
                    latestVersion = latest;
                    getLogger().warning("\u2554\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2557");
                    getLogger().warning("\u2551  New version available: " + latest + "         \u2551");
                    getLogger().warning("\u2551  Current version:       " + currentVersion + "         \u2551");
                    getLogger().warning("\u2551  modrinth.com/plugin/GvX5Lmcy  \u2551");
                    getLogger().warning("\u255a\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u255d");
                } else {
                    latestVersion = "";
                    getLogger().info("Plugin is up to date (" + currentVersion + ").");
                }
            } catch (Exception e) {
                latestVersion = "";
                getLogger().warning("Failed to check for updates: " + e.getMessage());
            }
        });
    }

    private boolean isNewerVersion(String remote, String current) {
        try {
            String[] r = remote.split("[.-]");
            String[] c = current.split("[.-]");
            int len = Math.max(r.length, c.length);
            for (int i = 0; i < len; i++) {
                int rv = i < r.length ? Integer.parseInt(r[i].replaceAll("[^0-9]", "0")) : 0;
                int cv = i < c.length ? Integer.parseInt(c[i].replaceAll("[^0-9]", "0")) : 0;
                if (rv > cv) return true;
                if (rv < cv) return false;
            }
            return false;
        } catch (Exception e) {
            return !remote.equals(current);
        }
    }

    private void registerCommand(@NotNull String name) {
        PluginCommand command = getCommand(name);
        if (command != null) command.setExecutor(this);
    }

    @Nullable
    private Player resolveTarget(@NotNull CommandSender sender, @NotNull String[] args, @NotNull String prefix) {
        Player target = findPlayer(args[0]);
        if (target == null) sender.sendMessage(prefix + "\u00a7c\u0418\u0433\u0440\u043e\u043a \u00a7f" + args[0] + " \u00a7c\u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d \u0438\u043b\u0438 \u043d\u0435 \u0432 \u0441\u0435\u0442\u0438!");
        return target;
    }

    @Nullable
    private Player findPlayer(@NotNull String name) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().equalsIgnoreCase(name)) return p;
        }
        return null;
    }
}
