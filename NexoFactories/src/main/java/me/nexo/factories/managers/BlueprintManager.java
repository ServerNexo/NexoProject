package me.nexo.factories.managers;

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

    // Memoria RAM temporal para saber quién está construyendo qué
    private final Map<UUID, List<BlockDisplay>> activeHolograms = new ConcurrentHashMap<>();
    private final Map<UUID, Location> activeCores = new ConcurrentHashMap<>();
    private final Map<UUID, StructureTemplate> activeTemplates = new ConcurrentHashMap<>();

    public BlueprintManager(NexoFactories plugin) {
        this.plugin = plugin;
    }

    public void projectBlueprint(Player player, Location coreLocation, StructureTemplate template) {
        clearBlueprint(player); // Limpiar cualquier plano viejo

        List<BlockDisplay> displays = new ArrayList<>();

        for (Map.Entry<Vector, Material> entry : template.getRequiredBlocks().entrySet()) {
            Vector rel = entry.getKey();
            Material mat = entry.getValue();

            Location displayLoc = coreLocation.clone().add(rel.getBlockX(), rel.getBlockY(), rel.getBlockZ());

            // Si el bloque ya está puesto en el mundo real, no ponemos el holograma
            if (displayLoc.getBlock().getType() == mat) continue;

            // Spawneamos la entidad visual (Cero lag, no tiene físicas)
            BlockDisplay display = (BlockDisplay) coreLocation.getWorld().spawnEntity(displayLoc, EntityType.BLOCK_DISPLAY);
            display.setBlock(Bukkit.createBlockData(mat));

            // Hacerlo "Fantasma" (Escalamos a 0.6 para que se vea como un mini-bloque flotando)
            display.setTransformation(new Transformation(
                    new org.joml.Vector3f(0.2f, 0.2f, 0.2f), // Centramos el bloque
                    new org.joml.Quaternionf(),
                    new org.joml.Vector3f(0.6f, 0.6f, 0.6f), // Escala
                    new org.joml.Quaternionf()
            ));
            display.setGlowing(true); // Le damos un contorno brillante
            displays.add(display);
        }

        activeHolograms.put(player.getUniqueId(), displays);
        activeCores.put(player.getUniqueId(), coreLocation);
        activeTemplates.put(player.getUniqueId(), template);

        player.sendMessage("§b§l[⚙] §fPlano proyectado. Coloca los bloques requeridos en el holograma.");
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

        // 1. Verificamos si el bloque que puso es parte de la estructura
        boolean isPart = false;
        for (Map.Entry<Vector, Material> entry : template.getRequiredBlocks().entrySet()) {
            Vector rel = entry.getKey();
            Location expectedLoc = coreLoc.clone().add(rel.getBlockX(), rel.getBlockY(), rel.getBlockZ());

            if (placedBlock.getLocation().equals(expectedLoc)) {
                if (placedBlock.getType() == entry.getValue()) {
                    isPart = true;

                    // Eliminar el holograma específico que estaba en esa posición
                    List<BlockDisplay> displays = activeHolograms.get(id);
                    if (displays != null) {
                        displays.removeIf(display -> {
                            if (display.getLocation().getBlockX() == placedBlock.getX() &&
                                    display.getLocation().getBlockY() == placedBlock.getY() &&
                                    display.getLocation().getBlockZ() == placedBlock.getZ()) {
                                display.remove();
                                return true;
                            }
                            return false;
                        });
                    }

                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 2f); // Sonido de "Pieza Encajada"
                } else {
                    player.sendMessage("§cPieza incorrecta. Debes colocar §e" + entry.getValue().name());
                    event.setCancelled(true);
                    return;
                }
                break;
            }
        }

        // 🌟 SI LA MÁQUINA FUE ENSAMBLADA COMPLETAMENTE
        if (isPart && template.isValid(coreLoc.getBlock())) {

            // 1. Buscamos la piedra en esa ubicación usando NexoProtections
            me.nexo.protections.core.ProtectionStone stone = me.nexo.protections.NexoProtections.getClaimManager().getStoneAt(coreLoc);

            if (stone == null) {
                player.sendMessage("§c§l¡ERROR! §cLas máquinas industriales solo pueden construirse dentro de una Zona Protegida.");
                player.sendMessage("§7Destruye la estructura y reconstrúyela cerca de tu Nexo.");
                event.setCancelled(true);
                return;
            }

            // 2. Vinculamos la Fábrica a la Piedra y la guardamos en el Grid
            ActiveFactory factory = new ActiveFactory(
                    UUID.randomUUID(),
                    stone.getStoneId(), // ⚡ ¡AQUÍ CONECTAMOS AL NEXO-GRID!
                    player.getUniqueId(),
                    template.getFactoryType(),
                    1, // Nivel 1 por defecto
                    "OFFLINE",
                    0,
                    coreLoc, // 🌟 Coordenadas del núcleo
                    "NONE",  // 🌟 Catalizador vacío por defecto
                    "NONE"   // 🌟 Script lógico vacío por defecto
            );
            plugin.getFactoryManager().createFactoryAsync(factory);

            player.sendMessage(" ");
            player.sendMessage("§a§l¡MÁQUINA ENSAMBLADA Y VINCULADA!");
            player.sendMessage("§7Estructura: §e" + template.getFactoryType());
            player.sendMessage("§7Red Eléctrica: §bConectada al Nexo Principal");
            player.sendMessage(" ");
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);

            clearBlueprint(player);
        }
    }
}