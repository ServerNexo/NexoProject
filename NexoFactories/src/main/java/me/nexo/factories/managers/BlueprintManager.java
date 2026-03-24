package me.nexo.factories.managers;

import me.nexo.core.utils.NexoColor;
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

    // 🎨 PALETA HEX
    private static final String MSG_BLUEPRINT_ON = "&#00fbff<bold>[⚙]</bold> &#a8ff78Plano proyectado. Coloca los materiales requeridos en el holograma.";
    private static final String ERR_WRONG_BLOCK = "&#ff4b2b[!] Pieza incorrecta o incompatible. El sistema requiere: &#fbd72b%block%";
    private static final String ERR_NO_STONE = "&#ff4b2b<bold>¡ERROR CRÍTICO!</bold> &#ff4b2bLas máquinas industriales solo pueden construirse dentro de una Zona Protegida. &#434343Destruye la estructura y reconstrúyela cerca de tu Nexo de Protección.";

    private static final String MSG_BUILT_TITLE = "&#a8ff78<bold>¡MÁQUINA ENSAMBLADA Y VINCULADA!</bold>";
    private static final String MSG_BUILT_TYPE = "&#434343Estructura Registrada: &#fbd72b%type%";
    private static final String MSG_BUILT_NET = "&#434343Red Eléctrica: &#00fbffConectada y Estable";

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

        player.sendMessage(NexoColor.parse(MSG_BLUEPRINT_ON));
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

                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 2f);
                } else {
                    player.sendMessage(NexoColor.parse(ERR_WRONG_BLOCK.replace("%block%", entry.getValue().name())));
                    event.setCancelled(true);
                    return;
                }
                break;
            }
        }

        if (isPart && template.isValid(coreLoc.getBlock())) {
            me.nexo.protections.core.ProtectionStone stone = me.nexo.protections.NexoProtections.getClaimManager().getStoneAt(coreLoc);

            if (stone == null) {
                player.sendMessage(NexoColor.parse(ERR_NO_STONE));
                event.setCancelled(true);
                return;
            }

            ActiveFactory factory = new ActiveFactory(
                    UUID.randomUUID(),
                    stone.getStoneId(),
                    player.getUniqueId(),
                    template.getFactoryType(),
                    1,
                    "OFFLINE",
                    0,
                    coreLoc,
                    "NONE",
                    "NONE"
            );
            plugin.getFactoryManager().createFactoryAsync(factory);

            player.sendMessage(NexoColor.parse(" "));
            player.sendMessage(NexoColor.parse(MSG_BUILT_TITLE));
            player.sendMessage(NexoColor.parse(MSG_BUILT_TYPE.replace("%type%", template.getFactoryType())));
            player.sendMessage(NexoColor.parse(MSG_BUILT_NET));
            player.sendMessage(NexoColor.parse(" "));
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);

            clearBlueprint(player);
        }
    }
}