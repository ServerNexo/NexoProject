package me.nexo.minions.listeners;

import me.nexo.core.utils.NexoColor;
import me.nexo.minions.NexoMinions;
import me.nexo.minions.data.MinionKeys;
import me.nexo.minions.manager.ActiveMinion;
import me.nexo.minions.menu.MinionMenu;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;

public class MenuListener implements Listener {
    private final NexoMinions plugin;

    public MenuListener(NexoMinions plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MinionMenu menu)) return;

        // Permitimos mover ítems en el propio inventario del jugador
        if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getWhoClicked().getInventory())) {
            if (event.isShiftClick()) guardarMejorasAsync(menu);
            return;
        }

        int slot = event.getRawSlot();
        ItemStack clickedItem = event.getCurrentItem();

        // Verificamos si hizo clic en un hueco de mejora
        boolean esSlotDeMejora = false;
        for (int s : MinionMenu.UPGRADE_SLOTS) {
            if (slot == s) esSlotDeMejora = true;
        }

        if (!esSlotDeMejora) {
            event.setCancelled(true);
        } else {
            // Si tocó una mejora, la guardamos instantáneamente
            guardarMejorasAsync(menu);
        }

        if (clickedItem == null) return;

        Player player = (Player) event.getWhoClicked();
        ActiveMinion minion = menu.getMinion();

        String plainName = "";
        if (clickedItem.hasItemMeta() && clickedItem.getItemMeta().hasDisplayName()) {
            plainName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(clickedItem.getItemMeta().displayName());
        }

        // 📦 Clic en Cosechar Tributo (Buscamos la Tolva)
        if (clickedItem.getType() == org.bukkit.Material.HOPPER && plainName.contains("COSECHAR")) {

            int cantidad = minion.getStoredItems();
            if (cantidad <= 0) {
                player.sendMessage(NexoColor.parse("&#FF3366[!] Herejía: &#E6CCFFLas fauces de la criatura están vacías."));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            // 🗜️ MOTOR HYPIXEL: LÓGICA DEL COMPACTADOR MATEMÁTICO
            boolean tieneCompactador = minion.tieneMejoraActiva("ITEM_UPGRADES");
            HashMap<Integer, ItemStack> sobrante = new HashMap<>();

            if (tieneCompactador) {
                int bloques = cantidad / 9;
                int sueltos = cantidad % 9;

                // Determinamos el material base y su forma compactada
                Material matBase = minion.getType().getTargetMaterial();
                Material matCompactado = obtenerBloqueCompactado(matBase);

                if (bloques > 0) {
                    sobrante.putAll(player.getInventory().addItem(new ItemStack(matCompactado, bloques)));
                }
                if (sueltos > 0) {
                    sobrante.putAll(player.getInventory().addItem(new ItemStack(matBase, sueltos)));
                }
            } else {
                sobrante.putAll(player.getInventory().addItem(new ItemStack(minion.getType().getTargetMaterial(), cantidad)));
            }

            // Tirar al suelo lo que no quepa en el inventario
            for (ItemStack drop : sobrante.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }

            // Entregar Experiencia
            String tipoMinion = minion.getType().name();
            double xpGanada = cantidad * 2.0;

            String skillAura = "";
            String nombreSkill = "";

            if (tipoMinion.contains("ORE") || tipoMinion.contains("COBBLESTONE") || tipoMinion.contains("STONE") || tipoMinion.contains("OBSIDIAN")) {
                skillAura = "mining"; nombreSkill = "Minería";
            } else if (tipoMinion.contains("WHEAT") || tipoMinion.contains("CARROT") || tipoMinion.contains("POTATO") || tipoMinion.contains("MELON") || tipoMinion.contains("PUMPKIN") || tipoMinion.contains("SUGAR_CANE")) {
                skillAura = "farming"; nombreSkill = "Agricultura";
            } else if (tipoMinion.contains("LOG") || tipoMinion.contains("WOOD")) {
                skillAura = "foraging"; nombreSkill = "Tala";
            } else if (tipoMinion.contains("FISH") || tipoMinion.contains("SALMON")) {
                skillAura = "fishing"; nombreSkill = "Pesca";
            } else if (tipoMinion.contains("FLESH") || tipoMinion.contains("BONE") || tipoMinion.contains("SPIDER") || tipoMinion.contains("GUNPOWDER") || tipoMinion.contains("SLIME") || tipoMinion.contains("BLAZE") || tipoMinion.contains("ROTTEN")) {
                skillAura = "fighting"; nombreSkill = "Combate";
            }

            if (!skillAura.isEmpty()) {
                // 🌟 RUTA DIRECTA: Usando el comando exacto que confirmaste en tu servidor (sin decimales)
                String comando = "skill xp add " + player.getName() + " " + skillAura + " " + (int)xpGanada + " silent";
                org.bukkit.Bukkit.getServer().dispatchCommand(org.bukkit.Bukkit.getConsoleSender(), comando);
                player.sendMessage(NexoColor.parse("&#9933FF✨ Conocimiento Arcano: +" + (int)xpGanada + " XP en " + nombreSkill));
            }

            // 🌟 PARCHE B: SINERGIA CON COLECCIONES (El trabajo del esclavo suma a tu progreso)
            if (org.bukkit.Bukkit.getPluginManager().getPlugin("NexoColecciones") != null) {
                me.nexo.colecciones.NexoColecciones.getPlugin(me.nexo.colecciones.NexoColecciones.class)
                        .getCollectionManager().addProgress(player, minion.getType().getTargetMaterial().name(), cantidad);
            }

            // Resetear Minion
            minion.setStoredItems(0);
            minion.getEntity().getPersistentDataContainer().set(MinionKeys.STORED_ITEMS, PersistentDataType.INTEGER, 0);

            // Mensaje adaptado si se usó el compactador
            player.sendMessage(NexoColor.parse("&#CC66FF[✓] <bold>TRIBUTO COSECHADO:</bold> &#E6CCFFHas reclamado las ofrendas" + (tieneCompactador ? " &#9933FF(Compactadas)" : "") + "."));
            player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1f, 2f);

            // 🌟 BEDROCK FIX: Cerrar menú y esperar antes de reabrir
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.openInventory(new MinionMenu(plugin, minion, player).getInventory());
            }, 3L);
        }

        // 🧨 Clic en Desterrar (Buscamos la Barrera)
        if (clickedItem.getType() == org.bukkit.Material.BARRIER && plainName.contains("DESTERRAR")) {

            player.closeInventory();
            plugin.getMinionManager().recogerMinion(player, minion.getEntity().getUniqueId());
        }

        // ⬆ Clic en Ascender / Evolucionar (Buscamos la Nether Star)
        if (clickedItem.getType() == org.bukkit.Material.NETHER_STAR) {
            int sigNivel = minion.getTier() + 1;
            if (sigNivel > 12) return; // Si ya es nivel máximo, el botón no hace nada

            ConfigurationSection costo = plugin.getTiersConfig().getCostoEvolucion(minion.getType(), sigNivel);
            if (costo == null) {
                player.sendMessage(NexoColor.parse("&#FF3366[!] Error Arcano: &#E6CCFFEl ritual no está en los textos sagrados."));
                return;
            }

            int reqCant = costo.getInt("cantidad");
            String reqMat = costo.getString("material", "");
            String reqNexo = costo.getString("nexo_id", "");

            boolean pagoExitoso = cobrarItems(player, reqCant, reqMat, reqNexo);

            if (!pagoExitoso) {
                player.sendMessage(NexoColor.parse("&#FF3366[!] Ritual Fallido: &#E6CCFFNo posees los sacrificios necesarios para la ascensión."));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            minion.setTier(sigNivel);
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.5f);
            player.sendMessage(NexoColor.parse("&#9933FF[✓] <bold>RITUAL COMPLETADO:</bold> &#E6CCFFEl esclavo ha ascendido a Nivel " + sigNivel + "."));
            minion.getEntity().getWorld().spawnParticle(org.bukkit.Particle.SCULK_SOUL, minion.getEntity().getLocation().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.1);

            // 🌟 BEDROCK FIX: Cerrar menú y esperar antes de reabrir
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.openInventory(new MinionMenu(plugin, minion, player).getInventory());
            }, 3L);
        }
    }

    // 🌟 PROTECCIÓN: Si arrastra ítems por el inventario (Drag)
    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof MinionMenu menu) {
            guardarMejorasAsync(menu);
        }
    }

    // Doble seguridad al cerrar
    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof MinionMenu menu)) return;
        guardarMejorasInstantaneo(menu, event.getInventory());
    }

    private void guardarMejorasAsync(MinionMenu menu) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            guardarMejorasInstantaneo(menu, menu.getInventory());
        }, 1L);
    }

    private void guardarMejorasInstantaneo(MinionMenu menu, org.bukkit.inventory.Inventory inv) {
        ActiveMinion minion = menu.getMinion();
        for (int i = 0; i < 4; i++) {
            int invSlot = MinionMenu.UPGRADE_SLOTS[i];
            ItemStack itemEnSlot = inv.getItem(invSlot);
            minion.setUpgrade(i, itemEnSlot);
        }
    }

    // 🗜️ MAPEO DE COMPACTADOR: Convierte material base a bloque
    private Material obtenerBloqueCompactado(Material matBase) {
        switch (matBase) {
            case COAL: return Material.COAL_BLOCK;
            case IRON_INGOT: return Material.IRON_BLOCK;
            case GOLD_INGOT: return Material.GOLD_BLOCK;
            case DIAMOND: return Material.DIAMOND_BLOCK;
            case EMERALD: return Material.EMERALD_BLOCK;
            case REDSTONE: return Material.REDSTONE_BLOCK;
            case LAPIS_LAZULI: return Material.LAPIS_BLOCK;
            case WHEAT: return Material.HAY_BLOCK;
            case SLIME_BALL: return Material.SLIME_BLOCK;
            case BONE: return Material.BONE_BLOCK;
            // Añade más aquí si tu servidor lo requiere
            default: return matBase; // Si no tiene versión compacta, da el material normal
        }
    }

    // Helper para cobrar ítems
    private boolean cobrarItems(Player player, int cantidadNecesaria, String material, String nexoId) {
        int recolectado = 0;
        NamespacedKey nexoKey = new NamespacedKey("nexo", "id");

        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType().isAir()) continue;

            boolean coincide = false;
            if (!nexoId.isEmpty() && item.hasItemMeta()) {
                String id = item.getItemMeta().getPersistentDataContainer().get(nexoKey, PersistentDataType.STRING);
                if (nexoId.equals(id)) coincide = true;
            } else if (nexoId.isEmpty() && item.getType().name().equalsIgnoreCase(material)) {
                coincide = true;
            }

            if (coincide) recolectado += item.getAmount();
        }

        if (recolectado < cantidadNecesaria) return false;

        int porCobrar = cantidadNecesaria;
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (porCobrar <= 0) break;
            if (item == null || item.getType().isAir()) continue;

            boolean coincide = false;
            if (!nexoId.isEmpty() && item.hasItemMeta()) {
                String id = item.getItemMeta().getPersistentDataContainer().get(nexoKey, PersistentDataType.STRING);
                if (nexoId.equals(id)) coincide = true;
            } else if (nexoId.isEmpty() && item.getType().name().equalsIgnoreCase(material)) {
                coincide = true;
            }

            if (coincide) {
                if (item.getAmount() <= porCobrar) {
                    porCobrar -= item.getAmount();
                    player.getInventory().setItem(i, null);
                } else {
                    item.setAmount(item.getAmount() - porCobrar);
                    porCobrar = 0;
                }
            }
        }
        return true;
    }
}