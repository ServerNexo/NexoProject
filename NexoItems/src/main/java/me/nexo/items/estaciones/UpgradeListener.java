package me.nexo.items.estaciones;

import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.skill.Skills;
import dev.aurelium.auraskills.api.user.SkillsUser;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.utils.NexoColor;
import me.nexo.items.NexoItems;
import me.nexo.items.managers.ItemManager;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class UpgradeListener implements Listener {

    private final NexoItems plugin;

    public UpgradeListener(NexoItems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();

        // 🌟 CORRECCIÓN: Obtenemos el título plano dinámicamente desde la config y el Crossplay
        String expectedTitle = PlainTextComponentSerializer.plainText().serialize(
                CrossplayUtils.parseCrossplay(player, plugin.getConfigManager().getMessage("menus.upgrade.titulo"))
        );
        String plainTitle = PlainTextComponentSerializer.plainText().serialize(event.getView().title());

        if (!plainTitle.equals(expectedTitle)) return;

        int slot = event.getSlot();

        if (event.getClickedInventory() == event.getView().getTopInventory() && slot != 13) {
            event.setCancelled(true);
        }

        if (slot == 22 && event.getClickedInventory() == event.getView().getTopInventory()) {
            ItemStack item = event.getView().getTopInventory().getItem(13);
            if (item == null || !item.hasItemMeta()) {
                player.sendMessage(NexoColor.parse("&#FF5555[!] Inserta un activo válido en la bahía de procesamiento."));
                return;
            }

            ItemMeta meta = item.getItemMeta();
            boolean isWeapon = meta.getPersistentDataContainer().has(ItemManager.llaveWeaponId, PersistentDataType.STRING);
            boolean isTool = meta.getPersistentDataContainer().has(ItemManager.llaveHerramientaId, PersistentDataType.STRING);

            if (!isWeapon && !isTool) {
                player.sendMessage(NexoColor.parse("&#FF5555[!] Este activo no soporta la Evolución Cénit."));
                return;
            }

            int nivelActual = meta.getPersistentDataContainer().getOrDefault(ItemManager.llaveNivelEvolucion, PersistentDataType.INTEGER, 1);

            // 🌟 LÍMITE ABSOLUTO DEL JUEGO
            if (nivelActual >= 60) {
                player.sendMessage(NexoColor.parse("&#FFAA00[!] El activo ya ha alcanzado su Cénit (Nivel Máximo 60)."));
                return;
            }

            // 🌟 VALIDACIÓN CONTRA LA RAMA DE HABILIDADES (AURASKILLS)
            int targetNivel = nivelActual + 1;
            int playerSkillLevel = 1;
            String skillName = "Habilidad";

            try {
                SkillsUser aUser = AuraSkillsApi.get().getUser(player.getUniqueId());
                if (aUser != null) {
                    if (isWeapon) {
                        playerSkillLevel = aUser.getSkillLevel(Skills.FIGHTING);
                        skillName = "Combate";
                    } else if (isTool) {
                        String mat = item.getType().name();
                        if (mat.contains("PICKAXE")) { playerSkillLevel = aUser.getSkillLevel(Skills.MINING); skillName = "Minería"; }
                        else if (mat.contains("AXE")) { playerSkillLevel = aUser.getSkillLevel(Skills.FORAGING); skillName = "Tala"; }
                        else if (mat.contains("HOE")) { playerSkillLevel = aUser.getSkillLevel(Skills.FARMING); skillName = "Agricultura"; }
                        else if (mat.contains("SPADE") || mat.contains("SHOVEL")) { playerSkillLevel = aUser.getSkillLevel(Skills.EXCAVATION); skillName = "Excavación"; }
                    }
                }
            } catch (Exception ignored) {
                // Fallback si AuraSkills falla
            }

            // Bloquear la evolución si el ítem superaría la maestría del jugador
            if (targetNivel > playerSkillLevel) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                player.sendMessage(NexoColor.parse("&#FF5555[!] Nivel de Operario Insuficiente. Requiere Nivel " + targetNivel + " en " + skillName + "."));
                return;
            }

            // ===============================================
            // 🪙 CÁLCULO DE DIVISAS (ESENCIAS VS FRAGMENTOS)
            // ===============================================
            int costo = targetNivel * 10;
            String divisaRequerida = isWeapon ? "Fragmentos de Nexo" : "Esencias de Vida";
            // Aquí iría tu validador de economía

            // Realizamos la mejora
            meta.getPersistentDataContainer().set(ItemManager.llaveNivelEvolucion, PersistentDataType.INTEGER, targetNivel);
            item.setItemMeta(meta);

            ItemManager.sincronizarItemAsync(item);

            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1.5f);
            player.sendMessage(NexoColor.parse("&#55FF55[✓] Evolución completada. El activo ha ascendido al Nivel " + targetNivel + "."));
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();

        // 🌟 CORRECCIÓN: Validación dinámica también al cerrar el inventario
        String expectedTitle = PlainTextComponentSerializer.plainText().serialize(
                CrossplayUtils.parseCrossplay(player, plugin.getConfigManager().getMessage("menus.upgrade.titulo"))
        );
        String plainTitle = PlainTextComponentSerializer.plainText().serialize(event.getView().title());

        if (plainTitle.equals(expectedTitle)) {
            ItemStack item = event.getView().getTopInventory().getItem(13);
            if (item != null && item.getType() != org.bukkit.Material.AIR) {
                player.getInventory().addItem(item);
            }
        }
    }
}