package me.nexo.minions.listeners;

import me.nexo.core.utils.NexoColor; // 🌟 IMPORT AÑADIDO PARA LA PALETA CIBERPUNK
import me.nexo.minions.NexoMinions;
import me.nexo.minions.data.MinionKeys;
import me.nexo.minions.manager.ActiveMinion;
import me.nexo.minions.menu.MinionMenu;
import org.bukkit.Bukkit;
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
            // Si hace shift-click, podría estar metiendo un ítem en las ranuras de mejora
            if (event.isShiftClick()) guardarMejorasAsync(menu);
            return;
        }

        int slot = event.getRawSlot();

        // Verificamos si hizo clic en un hueco de mejora
        boolean esSlotDeMejora = false;
        for (int s : MinionMenu.UPGRADE_SLOTS) {
            if (slot == s) esSlotDeMejora = true;
        }

        if (!esSlotDeMejora) {
            event.setCancelled(true);
        } else {
            // 🌟 NUEVO: Si tocó una mejora, la guardamos instantáneamente (1 tick después de que se coloque)
            guardarMejorasAsync(menu);
        }

        Player player = (Player) event.getWhoClicked();
        ActiveMinion minion = menu.getMinion();

        // 📦 Clic en Extraer Materiales (Slot 31)
        if (slot == 31) {
            int cantidad = minion.getStoredItems();
            if (cantidad <= 0) {
                player.sendMessage(NexoColor.parse("&#FF5555[!] Extracción Denegada: El depósito del operario está vacío."));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            HashMap<Integer, ItemStack> sobrante = player.getInventory().addItem(new ItemStack(minion.getType().getTargetMaterial(), cantidad));
            for (ItemStack drop : sobrante.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }

            // 🌟 2. ENTREGAR LA EXPERIENCIA (Soporte para 5 Profesiones de AuraSkills)
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
            } else if (tipoMinion.contains("FLESH") || tipoMinion.contains("BONE") || tipoMinion.contains("SPIDER") || tipoMinion.contains("GUNPOWDER") || tipoMinion.contains("SLIME") || tipoMinion.contains("BLAZE")) {
                skillAura = "fighting"; nombreSkill = "Combate";
            }

            if (!skillAura.isEmpty()) {
                String comando = "skills addxp " + player.getName() + " " + skillAura + " " + xpGanada + " -s";
                org.bukkit.Bukkit.getServer().dispatchCommand(org.bukkit.Bukkit.getConsoleSender(), comando);
                player.sendMessage(NexoColor.parse("&#AA00AA✨ Datos Procesados: +" + xpGanada + " XP en " + nombreSkill));
            }

            minion.setStoredItems(0);
            minion.getEntity().getPersistentDataContainer().set(MinionKeys.STORED_ITEMS, PersistentDataType.INTEGER, 0);

            player.sendMessage(NexoColor.parse("&#55FF55[✓] <bold>EXTRACCIÓN COMPLETADA:</bold> &#AAAAAASolicitaste " + cantidad + " unidades a la red."));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);

            player.openInventory(new MinionMenu(plugin, minion).getInventory());
        }

        // 🧨 Clic en Recoger Minion (Slot 35)
        if (slot == 35) {
            player.closeInventory();
            plugin.getMinionManager().recogerMinion(player, minion.getEntity().getUniqueId());
        }

        // ⬆ Clic en Evolucionar (Slot 22)
        if (slot == 22) {
            int sigNivel = minion.getTier() + 1;
            if (sigNivel > 12) return;

            ConfigurationSection costo = plugin.getTiersConfig().getCostoEvolucion(minion.getType(), sigNivel);
            if (costo == null) {
                player.sendMessage(NexoColor.parse("&#FF5555[!] Error del Sistema: El esquema de evolución no está registrado."));
                return;
            }

            int reqCant = costo.getInt("cantidad");
            String reqMat = costo.getString("material", "");
            String reqNexo = costo.getString("nexo_id", "");

            boolean pagoExitoso = cobrarItems(player, reqCant, reqMat, reqNexo);

            if (!pagoExitoso) {
                player.sendMessage(NexoColor.parse("&#FF5555[!] Recursos Insuficientes: &#AAAAAANo dispones de los materiales para instalar la mejora."));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            minion.setTier(sigNivel);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            player.sendMessage(NexoColor.parse("&#55FF55[✓] <bold>ACTUALIZACIÓN COMPLETADA:</bold> &#AAAAAAFirmware de operario ascendido a Nivel " + sigNivel + "."));
            minion.getEntity().getWorld().spawnParticle(org.bukkit.Particle.TOTEM_OF_UNDYING, minion.getEntity().getLocation().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.1);

            player.openInventory(new MinionMenu(plugin, minion).getInventory());
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

    // 🌟 NUEVO: Tarea asíncrona de 1 tick para leer el inventario DESPUÉS de que se coloque el ítem
    private void guardarMejorasAsync(MinionMenu menu) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            guardarMejorasInstantaneo(menu, menu.getInventory());
        }, 1L);
    }

    // Guarda las mejoras en la abeja
    private void guardarMejorasInstantaneo(MinionMenu menu, org.bukkit.inventory.Inventory inv) {
        ActiveMinion minion = menu.getMinion();
        for (int i = 0; i < 4; i++) {
            int invSlot = MinionMenu.UPGRADE_SLOTS[i];
            ItemStack itemEnSlot = inv.getItem(invSlot);
            minion.setUpgrade(i, itemEnSlot);
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