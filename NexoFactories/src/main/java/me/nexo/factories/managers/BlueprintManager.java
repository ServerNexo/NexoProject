package me.nexo.factories.managers;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.factories.NexoFactories;
import me.nexo.factories.core.ActiveFactory;
import me.nexo.factories.core.StructureTemplate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BlueprintManager implements Listener {

    private final NexoFactories plugin;

    private final Map<UUID, List<BlockDisplay>> activeHolograms = new ConcurrentHashMap<>();
    private final Map<UUID, Location> activeCores = new ConcurrentHashMap<>();
    private final Map<UUID, StructureTemplate> activeTemplates = new ConcurrentHashMap<>();

    public BlueprintManager(NexoFactories plugin) {
        this.plugin = plugin;
    }

    public void projectBlueprint(Player player, Location coreLocation, StructureTemplate template) {
        clearBlueprint(player);
        List<BlockDisplay> displays = new ArrayList<>();
        for (Map.Entry<Vector, Material> entry : template.getRequiredBlocks().entrySet()) {
            Vector rel = entry.getKey();
            Material mat = entry.getValue();
            Location displayLoc = coreLocation.clone().add(rel.getBlockX(), rel.getBlockY(), rel.getBlockZ());
            if (displayLoc.getBlock().getType() == mat) continue;
            BlockDisplay display = (BlockDisplay) coreLocation.getWorld().spawnEntity(displayLoc, EntityType.BLOCK_DISPLAY);
            display.setBlock(Bukkit.createBlockData(mat));
            display.setTransformation(new Transformation(
                    new org.joml.Vector3f(0.2f, 0.2f, 0.2f),
                    new org.joml.Quaternionf(),
                    new org.joml.Vector3f(0.6f, 0.6f, 0.6f),
                    new org.joml.Quaternionf()
            ));
            display.setGlowing(true);
            displays.add(display);
        }
        activeHolograms.put(player.getUniqueId(), displays);
        activeCores.put(player.getUniqueId(), coreLocation);
        activeTemplates.put(player.getUniqueId(), template);
        CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("eventos.blueprint.plano-proyectado"));
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 2f);
    }

    public void clearBlueprint(Player player) {
        List<BlockDisplay> displays = activeHolograms.remove(player.getUniqueId());
        activeCores.remove(player.getUniqueId());
        activeTemplates.remove(player.getUniqueId());
        if (displays != null) {
            for (BlockDisplay display : displays) {
                if (display.isValid()) display.remove();
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        if (!activeTemplates.containsKey(id)) return;

        Location coreLoc = activeCores.get(id);
        StructureTemplate template = activeTemplates.get(id);
        Block placedBlock = event.getBlockPlaced();

        boolean isPart = false;
        for (Map.Entry<Vector, Material> entry : template.getRequiredBlocks().entrySet()) {
            Vector rel = entry.getKey();
            Location expectedLoc = coreLoc.clone().add(rel.getBlockX(), rel.getBlockY(), rel.getBlockZ());
            if (placedBlock.getLocation().equals(expectedLoc)) {
                if (placedBlock.getType() == entry.getValue()) {
                    isPart = true;
                    activeHolograms.get(id).removeIf(display -> {
                        if (display.getLocation().getBlockX() == placedBlock.getX() &&
                                display.getLocation().getBlockY() == placedBlock.getY() &&
                                display.getLocation().getBlockZ() == placedBlock.getZ()) {
                            display.remove();
                            return true;
                        }
                        return false;
                    });
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 2f);
                } else {
                    CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("eventos.blueprint.pieza-incorrecta").replace("%block%", entry.getValue().name()));
                    event.setCancelled(true);
                    return;
                }
                break;
            }
        }

        if (isPart && template.isValid(coreLoc.getBlock())) {
            me.nexo.protections.core.ProtectionStone stone = me.nexo.protections.NexoProtections.getClaimManager().getStoneAt(coreLoc);
            if (stone == null) {
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("eventos.blueprint.sin-proteccion"));
                event.setCancelled(true);
                return;
            }

            ActiveFactory factory = new ActiveFactory(
                    UUID.randomUUID(), stone.getStoneId(), player.getUniqueId(),
                    template.getFactoryType(), 1, "OFFLINE", 0, coreLoc,
                    "NONE", "NONE", System.currentTimeMillis()
            );

            plugin.getFactoryManager().createFactoryAsync(factory).thenRun(() -> {
                CrossplayUtils.sendMessage(player, " ");
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("eventos.blueprint.maquina-ensamblada"));
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("eventos.blueprint.estructura-registrada").replace("%type%", template.getFactoryType()));
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("eventos.blueprint.red-electrica"));
                CrossplayUtils.sendMessage(player, " ");
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            });

            clearBlueprint(player);
        }
    }
}