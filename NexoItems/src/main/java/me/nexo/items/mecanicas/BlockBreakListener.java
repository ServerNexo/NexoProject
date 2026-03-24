package me.nexo.items.mecanicas;

import me.nexo.core.utils.NexoColor;
import me.nexo.items.managers.ItemManager;
import me.nexo.items.NexoItems;
import me.nexo.items.dtos.ArmorDTO;
import me.nexo.items.dtos.EnchantDTO;
import me.nexo.items.dtos.ToolDTO;
import me.nexo.core.user.NexoAPI;
import me.nexo.core.user.NexoUser;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BlockBreakListener implements Listener {

    private final NexoItems plugin;
    private final HashMap<UUID, Long> cooldownRecoleccion = new HashMap<>();
    private final String MUNDO_RPG = "Mina";
    private final Random random = new Random();

    public static final ConcurrentHashMap<Location, BlockData> bloquesRegenerando = new ConcurrentHashMap<>();

    public BlockBreakListener(NexoItems plugin) {
        this.plugin = plugin;
    }

    public static void restaurarBloquesRotos() {
        for (Map.Entry<Location, BlockData> entry : bloquesRegenerando.entrySet()) {
            entry.getKey().getBlock().setBlockData(entry.getValue());
        }
        bloquesRegenerando.clear();
    }

    private String serialize(String hex) {
        return LegacyComponentSerializer.legacySection().serialize(NexoColor.parse(hex));
    }

    @EventHandler
    public void alPonerBloque(BlockPlaceEvent event) {
        if (event.getPlayer().getWorld().getName().equalsIgnoreCase(MUNDO_RPG)) {
            if (!event.getPlayer().hasPermission("nexo.admin")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void alRomperBloque(BlockBreakEvent event) {
        Player jugador = event.getPlayer();
        Block bloque = event.getBlock();
        UUID uuid = jugador.getUniqueId();

        if (!jugador.getWorld().getName().equalsIgnoreCase(MUNDO_RPG)) return;

        Material tipoOriginal = bloque.getType();
        BlockData dataOriginal = bloque.getBlockData();

        int xpGanada = 0;
        int costeEnergia = 0;
        ItemStack recompensa = null;
        boolean esCultivo = false, esMineral = false, esTronco = false;

        if (tipoOriginal == Material.COAL_ORE || tipoOriginal == Material.DEEPSLATE_COAL_ORE) {
            xpGanada = 2; costeEnergia = 5; recompensa = new ItemStack(Material.COAL, 1); esMineral = true;
        } else if (tipoOriginal == Material.IRON_ORE || tipoOriginal == Material.DEEPSLATE_IRON_ORE) {
            xpGanada = 5; costeEnergia = 10; recompensa = new ItemStack(Material.RAW_IRON, 1); esMineral = true;
        } else if (tipoOriginal == Material.DIAMOND_ORE || tipoOriginal == Material.DEEPSLATE_DIAMOND_ORE) {
            xpGanada = 25; costeEnergia = 30; recompensa = new ItemStack(Material.DIAMOND, 1); esMineral = true;
        } else if (tipoOriginal == Material.OAK_LOG || tipoOriginal == Material.BIRCH_LOG || tipoOriginal == Material.SPRUCE_LOG) {
            xpGanada = 3; costeEnergia = 4; recompensa = new ItemStack(tipoOriginal, 1); esTronco = true;
        } else if (tipoOriginal == Material.WHEAT || tipoOriginal == Material.CARROTS || tipoOriginal == Material.POTATOES) {
            if (dataOriginal instanceof Ageable) {
                Ageable cultivo = (Ageable) dataOriginal;
                if (cultivo.getAge() == cultivo.getMaximumAge()) {
                    xpGanada = 1; costeEnergia = 1; esCultivo = true;
                    recompensa = new ItemStack(tipoOriginal == Material.WHEAT ? Material.WHEAT : (tipoOriginal == Material.CARROTS ? Material.CARROT : Material.POTATO), 1);
                } else { event.setCancelled(true); return; }
            }
        }

        if (xpGanada > 0) {
            event.setCancelled(true);

            long ahora = System.currentTimeMillis();
            if (cooldownRecoleccion.containsKey(uuid) && (ahora - cooldownRecoleccion.get(uuid)) < 300) return;

            NexoUser user = NexoAPI.getInstance().getUserLocal(uuid);
            if (user == null) {
                jugador.sendMessage(NexoColor.parse("&#FF5555[!] Enlace neural cargando. Por favor espera..."));
                return;
            }

            int energiaActual = user.getEnergiaMineria();
            if (energiaActual < costeEnergia) {
                jugador.sendActionBar(NexoColor.parse("&#FF5555<bold>⚠ ¡ENERGÍA AGOTADA!</bold> &#AAAAAADebes descansar..."));
                return;
            }

            double fortunaExtra = 0.0;
            String habilidadHerramienta = "ninguna";
            ItemStack itemMano = jugador.getInventory().getItemInMainHand();
            double suerteTotal = 0.0;

            if (itemMano != null && itemMano.hasItemMeta()) {
                ItemMeta metaTool = itemMano.getItemMeta();
                var pdc = metaTool.getPersistentDataContainer();

                if (pdc.has(ItemManager.llaveHerramientaId, PersistentDataType.STRING)) {
                    String toolId = pdc.get(ItemManager.llaveHerramientaId, PersistentDataType.STRING);
                    ToolDTO toolData = plugin.getFileManager().getToolDTO(toolId);

                    if (toolData != null) {
                        fortunaExtra = toolData.multiplicadorFortuna();
                        habilidadHerramienta = toolData.habilidadId();

                        NamespacedKey keyBendicion = new NamespacedKey(plugin, "nexo_enchant_bendicion_nexo");
                        if (pdc.has(keyBendicion, PersistentDataType.INTEGER)) {
                            EnchantDTO ench = plugin.getFileManager().getEnchantDTO("bendicion_nexo");
                            if (ench != null) fortunaExtra += ench.getValorPorNivel(pdc.get(keyBendicion, PersistentDataType.INTEGER));
                        }

                        NamespacedKey keyExp = new NamespacedKey(plugin, "nexo_enchant_experiencia_divina");
                        if (pdc.has(keyExp, PersistentDataType.INTEGER)) {
                            EnchantDTO ench = plugin.getFileManager().getEnchantDTO("experiencia_divina");
                            if (ench != null) xpGanada = (int) (xpGanada * ench.getValorPorNivel(pdc.get(keyExp, PersistentDataType.INTEGER)));
                        }

                        NamespacedKey keyMidas = new NamespacedKey(plugin, "nexo_enchant_toque_de_midas");
                        if (pdc.has(keyMidas, PersistentDataType.INTEGER)) {
                            EnchantDTO ench = plugin.getFileManager().getEnchantDTO("toque_de_midas");
                            if (ench != null && (random.nextDouble() * 100 <= ench.getValorPorNivel(pdc.get(keyMidas, PersistentDataType.INTEGER)))) {
                                ItemStack oro = new ItemStack(Material.GOLD_INGOT);
                                ItemMeta oroMeta = oro.getItemMeta();
                                if (oroMeta != null) {
                                    oroMeta.setDisplayName(serialize("&#FFAA00✨ Oro Sintético"));
                                    oro.setItemMeta(oroMeta);
                                }
                                jugador.getInventory().addItem(oro);
                                jugador.playSound(jugador.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2f);
                            }
                        }

                        Integer rotosGuardados = pdc.get(ItemManager.llaveBloquesRotos, PersistentDataType.INTEGER);
                        int rotos = (rotosGuardados != null ? rotosGuardados : 0) + 1;
                        pdc.set(ItemManager.llaveBloquesRotos, PersistentDataType.INTEGER, rotos);

                        List<String> lore = metaTool.getLore();
                        if (lore != null) {
                            for (int i = 0; i < lore.size(); i++) {
                                // 🌟 Actualización limpia del Lore
                                if (org.bukkit.ChatColor.stripColor(lore.get(i)).contains("Bloques Rotos:")) {
                                    lore.set(i, serialize("&#AAAAAABloques Rotos: &#FFAA00" + String.format("%,d", rotos)));
                                    break;
                                }
                            }
                            metaTool.setLore(lore);
                        }
                        itemMano.setItemMeta(metaTool);
                    }
                }
            }

            suerteTotal = fortunaExtra;
            for (ItemStack armor : jugador.getInventory().getArmorContents()) {
                if (armor == null || !armor.hasItemMeta()) continue;
                var pdc = armor.getItemMeta().getPersistentDataContainer();

                if (pdc.has(ItemManager.llaveArmaduraId, PersistentDataType.STRING)) {
                    ArmorDTO armorDTO = plugin.getFileManager().getArmorDTO(pdc.get(ItemManager.llaveArmaduraId, PersistentDataType.STRING));
                    if (armorDTO != null) {
                        if (esMineral) suerteTotal += armorDTO.suerteMinera();
                        if (esCultivo) suerteTotal += armorDTO.suerteAgricola();
                        if (esTronco) suerteTotal += armorDTO.suerteTala();
                    }
                }

                NamespacedKey keyAura = new NamespacedKey(plugin, "nexo_enchant_aura_recolectora");
                if (pdc.has(keyAura, PersistentDataType.INTEGER)) {
                    EnchantDTO ench = plugin.getFileManager().getEnchantDTO("aura_recolectora");
                    if (ench != null) suerteTotal += ench.getValorPorNivel(pdc.get(keyAura, PersistentDataType.INTEGER));
                }
            }

            // 🎲 CÁLCULO DE DROPS CENTRAL
            int cantidad = (random.nextDouble() * 100 <= suerteTotal) ? 2 : 1;
            if (cantidad > 1) {
                jugador.sendActionBar(NexoColor.parse("&#00E5FF✨ ¡DOBLE MATERIAL! &#555555(+" + String.format("%.1f", suerteTotal) + "%)"));
            }

            if (recompensa != null) {
                recompensa.setAmount(cantidad);
                jugador.getInventory().addItem(recompensa);
            }

            // 🟢 GUARDADO DE ENERGÍA Y XP EN NEXOUSER
            user.setEnergiaMineria(Math.max(0, energiaActual - costeEnergia));
            cooldownRecoleccion.put(uuid, ahora);

            int nivelActual = user.getNexoNivel();
            int xpActual = user.getNexoXp() + xpGanada;

            while (xpActual >= (nivelActual * 100)) {
                xpActual -= (nivelActual * 100);
                nivelActual++;
                jugador.sendTitle(serialize("&#FFAA00<bold>¡ASCENSO CÉNIT!</bold>"), serialize("&#AAAAAANivel Corporal: &#00E5FF" + nivelActual), 10, 70, 20);
            }

            user.setNexoNivel(nivelActual);
            user.setNexoXp(xpActual);

            // ==========================================
            // ⚡ EJECUCIÓN DE HABILIDADES DE HERRAMIENTA
            // ==========================================
            ejecutarHabilidadHerramienta(jugador, bloque, tipoOriginal, dataOriginal, habilidadHerramienta, recompensa.clone(), suerteTotal, esTronco);

            // ♻️ REGENERACIÓN DEL BLOQUE CENTRAL
            if (esCultivo) {
                if (!habilidadHerramienta.equalsIgnoreCase("replante_auto")) {
                    Ageable cultivoNuevo = (Ageable) dataOriginal.clone();
                    cultivoNuevo.setAge(0);
                    bloque.setBlockData(cultivoNuevo);
                }
            } else {
                bloquesRegenerando.put(bloque.getLocation(), dataOriginal);
                bloque.setType(esTronco ? Material.AIR : Material.BEDROCK);

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (bloquesRegenerando.containsKey(bloque.getLocation())) {
                        bloque.setBlockData(dataOriginal);
                        bloquesRegenerando.remove(bloque.getLocation());
                    }
                }, 200L); // 10 segundos
            }

        } else if (!jugador.hasPermission("nexo.admin")) {
            event.setCancelled(true);
        }
    }

    private void ejecutarHabilidadHerramienta(Player p, Block bloqueCentral, Material tipoOriginal, BlockData dataOriginal, String habilidad, ItemStack recompensaBase, double suerteTotal, boolean esTronco) {
        if (habilidad == null) return;

        switch (habilidad.toLowerCase()) {
            case "treecapitator":
                if (!esTronco) break;
                int tumbados = 0;
                Block actual = bloqueCentral.getRelative(BlockFace.UP);
                while (actual.getType() == tipoOriginal && tumbados < 8) {
                    procesarBloqueExtra(p, actual, tipoOriginal, dataOriginal, recompensaBase, suerteTotal, true);
                    actual = actual.getRelative(BlockFace.UP);
                    tumbados++;
                }
                if (tumbados > 0) p.playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.5f, 1.5f);
                break;

            case "vein_miner":
                int minados = 0;
                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        for (int z = -1; z <= 1; z++) {
                            if (x == 0 && y == 0 && z == 0) continue;
                            if (minados >= 6) break;

                            Block cercano = bloqueCentral.getRelative(x, y, z);
                            if (cercano.getType() == tipoOriginal) {
                                procesarBloqueExtra(p, cercano, tipoOriginal, dataOriginal, recompensaBase, suerteTotal, false);
                                minados++;
                            }
                        }
                    }
                }
                if (minados > 0) p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1f, 1f);
                break;

            case "replante_auto":
                if (dataOriginal instanceof Ageable) {
                    Ageable cultivoNuevo = (Ageable) dataOriginal.clone();
                    cultivoNuevo.setAge(0);
                    bloqueCentral.setBlockData(cultivoNuevo);
                    p.playSound(p.getLocation(), Sound.ITEM_CROP_PLANT, 1f, 1f);
                }
                break;

            case "rompe_3x3":
                int rotos = 0;
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && z == 0) continue;
                        Block cercano = bloqueCentral.getRelative(x, 0, z);
                        if (cercano.getType() == tipoOriginal) {
                            procesarBloqueExtra(p, cercano, tipoOriginal, dataOriginal, recompensaBase, suerteTotal, false);
                            rotos++;
                        }
                    }
                }
                if (rotos > 0) p.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 2f);
                break;
        }
    }

    private void procesarBloqueExtra(Player p, Block b, Material tipoOriginal, BlockData dataOriginal, ItemStack recompensaBase, double suerteTotal, boolean esTronco) {
        int cantidad = (random.nextDouble() * 100 <= suerteTotal) ? 2 : 1;
        ItemStack recompensaFinal = recompensaBase.clone();
        recompensaFinal.setAmount(cantidad);

        p.getInventory().addItem(recompensaFinal);

        bloquesRegenerando.put(b.getLocation(), dataOriginal);
        b.setType(esTronco ? Material.AIR : Material.BEDROCK);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (bloquesRegenerando.containsKey(b.getLocation())) {
                b.setBlockData(dataOriginal);
                bloquesRegenerando.remove(b.getLocation());
            }
        }, 200L); // 10 segundos
    }
}