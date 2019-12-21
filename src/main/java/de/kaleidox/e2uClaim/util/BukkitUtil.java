package de.kaleidox.e2uClaim.util;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import de.kaleidox.e2uClaim.E2UClaim;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.EventExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class BukkitUtil {
    private BukkitUtil() {
    }

    public static Optional<Player> getPlayer(CommandSender cmdSender) {
        if (cmdSender instanceof Player) return Optional.of((Player) cmdSender);

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.getName().equals(cmdSender.getName()))
                return Optional.of(onlinePlayer);
        }

        return Optional.empty();
    }

    public static CompletableFuture<String> inputFromPlayer(String title, Player player) {
        class Local implements Listener, EventExecutor {
            private final CompletableFuture<String> input;
            private final Player player;

            public Local(Player player) {
                this.input = new CompletableFuture<>();
                this.player = player;

                input.thenRun(() -> HandlerList.unregisterAll(this));
            }

            @EventHandler
            public void onInventoryClick(InventoryClickEvent event) {
                if (event.getWhoClicked().equals(this.player)) {
                    final ItemStack stack = event.getCurrentItem();

                    if (stack != null) {
                        final ItemMeta itemMeta = stack.getItemMeta();

                        if (itemMeta != null)
                            if (itemMeta.hasDisplayName())
                                this.input.complete(itemMeta.getDisplayName());
                            else this.input.completeExceptionally(new NullPointerException("Item has no display name"));
                    }
                }
            }

            @Override
            public void execute(@NotNull Listener listener, @NotNull Event event) {
                if (listener instanceof Local && event instanceof InventoryClickEvent)
                    ((Local) listener).onInventoryClick((InventoryClickEvent) event);
            }
        }

        final Local local = new Local(player);

        final ItemStack paperStack = new ItemStack(Material.PAPER, 1);
        final ItemMeta itemMeta = Objects.requireNonNull(paperStack.getItemMeta(), "PaperStack Meta is null!");
        itemMeta.setDisplayName(title);

        final @NotNull Inventory anvil = Bukkit.createInventory(player, InventoryType.ANVIL);
        ReflectionUtil.printMethods(anvil, System.out);

        Bukkit.getPluginManager().registerEvent(InventoryClickEvent.class, local, EventPriority.HIGHEST, local, E2UClaim.INSTANCE, false);

        return local.input;
    }

    public static Optional<Material> getMaterial(@Nullable String name) {
        if (name == null) return Optional.empty();

        Material val = Material.getMaterial(name);
        if (val != null) return Optional.of(val);

        for (Material value : Material.values())
            if (value.name().equalsIgnoreCase(name))
                return Optional.of(value);
        return Optional.empty();
    }

    public static int getNumericPermissionValue(
            E2UClaim.Permission permission,
            Permissible entity,
            Supplier<Integer> fallback
    ) {
        if (entity.hasPermission(permission.node + "*"))
            return Integer.MAX_VALUE;
        return entity.getEffectivePermissions()
                .stream()
                .filter(perm -> perm.getPermission().indexOf(permission.node) == 0)
                .findFirst()
                .map(PermissionAttachmentInfo::getPermission)
                .map(str -> {
                    String[] split = str.split("\\.");
                    return split[split.length - 1];
                })
                .map(Integer::parseInt)
                .orElseGet(fallback);
    }
}
