package me.nexo.dungeons.listeners;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import me.nexo.dungeons.NexoDungeons;
import me.nexo.dungeons.data.EventRule;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DungeonListener implements Listener {

    private final NexoDungeons plugin;
    private final Map<UUID, Long> antiSpamCooldown = new ConcurrentHashMap<>();
    private final Map<String, Integer> globalCounters = new ConcurrentHashMap<>();

    public DungeonListener(NexoDungeons plugin) {
        this.plugin = plugin;
    }

    // =========================================
    // 🖱️ 1. INTERACTUAR
    // =========================================
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.PHYSICAL) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        procesarEventoMotor(event.getPlayer(), block, "PLAYER_INTERACT", event);
    }

    // =========================================
    // ⛏️ 2. ROMPER BLOQUES
    // =========================================
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        procesarEventoMotor(event.getPlayer(), event.getBlock(), "BLOCK_BREAK", event);
    }

    // =========================================
    // 🧠 3. EL CEREBRO DEL MOTOR
    // =========================================
    private void procesarEventoMotor(Player p, Block block, String triggerType, org.bukkit.event.Cancellable bukkitEvent) {
        EventRule rule = plugin.getPuzzleEngine().getRuleAt(block.getLocation());
        if (rule == null) return;

        if (!rule.trigger().type().equalsIgnoreCase(triggerType)) return;

        if (!rule.trigger().material().equalsIgnoreCase("ANY") && !rule.trigger().material().equalsIgnoreCase(block.getType().name())) {
            return;
        }

        long now = System.currentTimeMillis();
        if (antiSpamCooldown.containsKey(p.getUniqueId()) && (now - antiSpamCooldown.get(p.getUniqueId()) < 100)) {
            bukkitEvent.setCancelled(true);
            return;
        }
        antiSpamCooldown.put(p.getUniqueId(), now);
        bukkitEvent.setCancelled(true);

        // Pasamos también el objeto 'rule' para poder calcular los Offsets
        Thread.startVirtualThread(() -> {
            for (EventRule.Action action : rule.actions()) {
                ejecutarAccion(p, action, block.getLocation(), rule);
            }
        });
    }

    // =========================================
    // 🎬 4. EJECUTOR DE ACCIONES
    // =========================================
    private void ejecutarAccion(Player p, EventRule.Action action, Location baseLoc, EventRule rule) {
        try {
            switch (action.type().toUpperCase()) {

                // 🔊 Reproducir Sonido
                case "PLAY_SOUND" -> {
                    float volume = action.volume() != null ? action.volume().floatValue() : 1.0f;
                    float pitch = action.pitch() != null ? action.pitch().floatValue() : 1.0f;

                    // 🌟 PAPER 1.21+: Adaptación al nuevo sistema de Registros (Registry)
                    // Convertimos el texto del JSON (ej: "BLOCK_PORTAL_TRAVEL") a la llave nativa ("block.portal.travel")
                    String soundKey = action.sound().toLowerCase().replace("_", ".");
                    Sound sound = org.bukkit.Registry.SOUNDS.get(org.bukkit.NamespacedKey.minecraft(soundKey));

                    if (sound != null) {
                        Bukkit.getScheduler().runTask(plugin, () -> baseLoc.getWorld().playSound(baseLoc, sound, volume, pitch));
                    } else {
                        plugin.getLogger().warning("⚠️ El sonido '" + action.sound() + "' no se encontró en el registro de Minecraft.");
                    }
                }

                // 📦 Consumir Ítem
                case "CONSUME_NEXO_ITEM" -> {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        boolean consumed = false;
                        String targetId = action.itemId();
                        int amountNeeded = action.amount() != null ? action.amount() : 1;

                        for (ItemStack item : p.getInventory().getContents()) {
                            if (item != null && item.getType().name().equalsIgnoreCase(targetId)) {
                                if (item.getAmount() >= amountNeeded) {
                                    item.setAmount(item.getAmount() - amountNeeded);
                                    consumed = true;
                                    break;
                                }
                            }
                        }
                        if (!consumed) p.sendMessage("§cNo tienes el ítem necesario para activar esto.");
                    });
                }

                // 🐉 Invocar Boss
                case "SPAWN_MYTHICMOB" -> {
                    String mobId = action.mobId();

                    // Cálculo de Offset por si es Instanciado
                    int offsetX = rule.isInstanced() ? (baseLoc.getBlockX() - rule.trigger().loc().get("x")) : 0;
                    int offsetZ = rule.isInstanced() ? (baseLoc.getBlockZ() - rule.trigger().loc().get("z")) : 0;

                    double x = action.loc().getOrDefault("x", baseLoc.getBlockX()) + offsetX;
                    double y = action.loc().getOrDefault("y", baseLoc.getBlockY());
                    double z = action.loc().getOrDefault("z", baseLoc.getBlockZ()) + offsetZ;
                    Location spawnLoc = new Location(baseLoc.getWorld(), x, y, z);

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        io.lumine.mythic.bukkit.MythicBukkit.inst().getMobManager().spawnMob(mobId, spawnLoc);
                    });
                }

                // 🧱 REEMPLAZAR BLOQUES CON FAWE (¡Magia Asíncrona!)
                case "REPLACE_BLOCKS" -> {
                    Map<String, Integer> loc1 = action.loc1();
                    Map<String, Integer> loc2 = action.loc2();
                    if (loc1 == null || loc2 == null) return;

                    // 1. Calculamos cuánto se movió el schematic desde su punto original de creación
                    int offsetX = 0, offsetY = 0, offsetZ = 0;
                    if (rule.isInstanced()) {
                        offsetX = baseLoc.getBlockX() - rule.trigger().loc().get("x");
                        offsetY = baseLoc.getBlockY() - rule.trigger().loc().get("y");
                        offsetZ = baseLoc.getBlockZ() - rule.trigger().loc().get("z");
                    }

                    // 2. Aplicamos el Offset a las coordenadas de la pared que va a desaparecer
                    int x1 = loc1.get("x") + offsetX;
                    int y1 = loc1.get("y") + offsetY;
                    int z1 = loc1.get("z") + offsetZ;

                    int x2 = loc2.get("x") + offsetX;
                    int y2 = loc2.get("y") + offsetY;
                    int z2 = loc2.get("z") + offsetZ;

                    // 3. Obtenemos el material de Bukkit y lo adaptamos para WorldEdit
                    Material material = Material.valueOf(action.material().toUpperCase());
                    com.sk89q.worldedit.world.block.BlockState weBlock = BukkitAdapter.adapt(material.createBlockData());
                    com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(baseLoc.getWorld());

                    // 4. FAWE EditSession: Edición de cuboides sin lag
                    try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
                        CuboidRegion region = new CuboidRegion(
                                weWorld,
                                BlockVector3.at(x1, y1, z1),
                                BlockVector3.at(x2, y2, z2)
                        );
                        // Borramos/ponemos los bloques en la región instantáneamente
                        editSession.setBlocks(region, weBlock);
                        plugin.getLogger().info("🧱 FAWE: Reemplazados bloques en " + x1 + "," + y1 + "," + z1);
                    }
                }

                // ⚔️ Iniciar Oleadas
                case "START_WAVE_ARENA" -> {
                    String arenaId = action.counterId();
                    Bukkit.getScheduler().runTask(plugin, () -> plugin.getWaveManager().startArena(arenaId, baseLoc));
                }

                default -> plugin.getLogger().warning("⚠️ Acción desconocida: " + action.type());
            }
        } catch (Exception e) {
            plugin.getLogger().severe("❌ Error ejecutando acción " + action.type() + ": " + e.getMessage());
        }
    }

    // ==========================================
    // 🧹 PREVENCIÓN DE FUGAS DE MEMORIA (RAM)
    // ==========================================
    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        antiSpamCooldown.remove(event.getPlayer().getUniqueId());
        globalCounters.remove(event.getPlayer().getUniqueId().toString()); // Opcional limpieza
    }
}