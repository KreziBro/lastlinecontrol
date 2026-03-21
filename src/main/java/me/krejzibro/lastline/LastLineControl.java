package me.krejzibro.lastline;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.HttpURLConnection;
import java.net.URI;
import java.util.HashSet;
import java.util.Scanner;
import java.util.UUID;

@SuppressWarnings("unused")
public class LastLineControl extends JavaPlugin implements Listener, CommandExecutor {
    private final HashSet<UUID> activeGod = new HashSet<>();

    private static final String PROJECT_ID = "GvX5Lmcy";

    private String latestVersion = null;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        Bukkit.getPluginManager().registerEvents(this, this);

        registerCommand("god");
        registerCommand("fly");
        registerCommand("llc");

        if (getConfig().getBoolean("settings.check-updates", true)) {
            checkUpdates();
        } else {
            getLogger().info("Update checker is disabled in config.");
        }
    }

    @SuppressWarnings("deprecation")
    private void checkUpdates() {
        String currentVersion = getDescription().getVersion();

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                java.net.URL url = new URI(
                        "https://api.modrinth.com/v2/project/" + PROJECT_ID + "/version"
                ).toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", "LastLineControl/" + currentVersion);

                Scanner scanner = new Scanner(conn.getInputStream());
                String response = scanner.useDelimiter("\\A").next();
                scanner.close();

                String latest = response.split("\"version_number\":\"")[1].split("\"")[0];

                if (!latest.equals(currentVersion)) {
                    latestVersion = latest;
                    getLogger().warning("╔══════════════════════════════════════╗");
                    getLogger().warning("║  New version available: " + latest + "         ║");
                    getLogger().warning("║  Current version:       " + currentVersion + "         ║");
                    getLogger().warning("║  modrinth.com/plugin/" + PROJECT_ID + "  ║");
                    getLogger().warning("╚══════════════════════════════════════╝");
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

    @EventHandler
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("lastline.admin")) return;
        if (!getConfig().getBoolean("settings.check-updates", true)) return;
        if (latestVersion == null || latestVersion.isEmpty()) return;

        Bukkit.getScheduler().runTaskLater(this, () -> {
            player.sendMessage("§6[LastLineControl] §fДоступна новая версия: §a" + latestVersion);
            player.sendMessage("§6[LastLineControl] §fСкачать: §bhttps://modrinth.com/plugin/" + PROJECT_ID);
        }, 40L);
    }

    private void registerCommand(@NotNull String name) {
        PluginCommand command = getCommand(name);
        if (command != null) {
            command.setExecutor(this);
        }
    }

    @Nullable
    private Player findPlayer(@NotNull String name) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().equalsIgnoreCase(name)) {
                return p;
            }
        }
        return null;
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        @Nullable String rawPrefix = getConfig().getString("messages.prefix");
        String prefix = (rawPrefix != null ? rawPrefix : "§6[Сервер] ").replace("&", "§");

        if (cmd.getName().equalsIgnoreCase("llc")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("lastline.admin")) {
                    sender.sendMessage(prefix + "§cУ вас нет прав!");
                    return true;
                }
                reloadConfig();
                sender.sendMessage(prefix + "§aКонфигурация успешно перезагружена!");
                return true;
            }
            sender.sendMessage(prefix + "§fИспользуйте: §6/llc reload");
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("god")) {

            if (args.length > 0) {
                if (!sender.hasPermission("lastline.admin")) {
                    sender.sendMessage(prefix + "§cУ вас нет прав для выдачи режима бога другим игрокам!");
                    return true;
                }
                Player target = findPlayer(args[0]);
                if (target == null) {
                    sender.sendMessage(prefix + "§cИгрок §f" + args[0] + " §cне найден или не в сети!");
                    return true;
                }
                toggleGod(target, prefix);
                String statusWord = activeGod.contains(target.getUniqueId()) ? "§aвключён" : "§cвыключен";
                sender.sendMessage(prefix + "§fРежим бога для §6" + target.getName() + " §f" + statusWord);
                return true;
            }

            if (!(sender instanceof Player player)) {
                sender.sendMessage(prefix + "§cКонсоль должна указать ник: §f/god <ник>");
                return true;
            }
            if (!player.hasPermission("lastline.god")) {
                player.sendMessage(prefix + "§cУ вас нет прав на использование этой команды!");
                return true;
            }
            toggleGod(player, prefix);
            return true;
        }
        
        if (cmd.getName().equalsIgnoreCase("fly")) {

            if (args.length > 0) {
                if (!sender.hasPermission("lastline.admin")) {
                    sender.sendMessage(prefix + "§cУ вас нет прав для выдачи полёта другим игрокам!");
                    return true;
                }
                Player target = findPlayer(args[0]);
                if (target == null) {
                    sender.sendMessage(prefix + "§cИгрок §f" + args[0] + " §cне найден или не в сети!");
                    return true;
                }
                toggleFly(target, prefix);
                String statusWord = target.getAllowFlight() ? "§aвключён" : "§cвыключен";
                sender.sendMessage(prefix + "§fРежим полёта для §6" + target.getName() + " §f" + statusWord);
                return true;
            }

            if (!(sender instanceof Player player)) {
                sender.sendMessage(prefix + "§cКонсоль должна указать ник: §f/fly <ник>");
                return true;
            }
            if (!player.hasPermission("lastline.fly")) {
                player.sendMessage(prefix + "§cУ вас нет прав на использование этой команды!");
                return true;
            }
            toggleFly(player, prefix);
            return true;
        }

        return true;
    }

    private void toggleGod(@NotNull Player player, @NotNull String prefix) {
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            player.sendMessage(prefix + "§cВы в креативе/наблюдателе и не можете использовать режим бога!");
            return;
        }
        if (activeGod.contains(player.getUniqueId())) {
            activeGod.remove(player.getUniqueId());
            player.setInvulnerable(false);
            player.sendMessage(prefix + "§fРежим бога §cвыключен");
        } else {
            activeGod.add(player.getUniqueId());
            player.setInvulnerable(true);
            player.sendMessage(prefix + "§fРежим бога §aвключен");
        }
    }

    private void toggleFly(@NotNull Player player, @NotNull String prefix) {
        if (player.getGameMode() == GameMode.SPECTATOR) {
            player.sendMessage(prefix + "§cВ режиме наблюдателя полёт нельзя изменить!");
            return;
        }
        boolean canFlyBefore = player.getAllowFlight();
        player.setAllowFlight(!canFlyBefore);
        if (canFlyBefore && player.isFlying()) {
            player.setFlying(false);
        }
        String status = !canFlyBefore ? "§aвключен" : "§cвыключен";
        player.sendMessage(prefix + "§fРежим полёта " + status);
    }

    @EventHandler
    public void onPickup(@NotNull EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (player.hasPermission("lastline.bypass.pickup")) return;
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;

        if (activeGod.contains(player.getUniqueId()) && getConfig().getBoolean("settings.disable-pickup-in-god", true)) {
            event.setCancelled(true);
            player.sendActionBar(Component.text("§fВы не можете подобрать вы находитесь в режиме §6/god"));
            return;
        }

        if (player.isFlying() && getConfig().getBoolean("settings.disable-pickup-in-fly", true)) {
            event.setCancelled(true);
            player.sendActionBar(Component.text("§fВы не можете подобрать вы находитесь в режиме §6/fly"));
        }
    }
}