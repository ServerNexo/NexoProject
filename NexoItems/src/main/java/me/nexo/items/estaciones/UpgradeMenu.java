package me.nexo.items.estaciones;

import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.skill.Skills;
import dev.aurelium.auraskills.api.user.SkillsUser;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
import me.nexo.core.utils.NexoColor;
import me.nexo.items.NexoItems;
import me.nexo.items.managers.ItemManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.stream.Collectors;

public class UpgradeMenu extends NexoMenu {

    private final NexoItems plugin;

    public UpgradeMenu(Player player, NexoItems plugin) {
        super(player);
        this.plugin = plugin;
    }

    @Override
    public String getMenuName() {
        return plugin.getConfigManager().getMessage("menus.upgrade.titulo");
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void setMenuItems() {
        setFillerGlass(); // El fondo morado del Vacío

        // El slot 13 es la bahía de procesamiento (se deja vacío intencionalmente)
        inventory.setItem(13, null);

        // Botón de Evolución
        ItemStack btn = new ItemStack(Material.NETHER_STAR);
        ItemMeta btnMeta = btn.getItemMeta();
        if (btnMeta != null) {
            btnMeta.displayName(CrossplayUtils.parseCrossplay(player, plugin.getConfigManager().getMessage("menus.upgrade.boton.titulo")));

            List<String> loreConfig = plugin.getConfigManager().getMessages().getStringList("menus.upgrade.boton.lore");
            btnMeta.lore(loreConfig.stream()
                    .map(line -> CrossplayUtils.parseCrossplay(player, line))
                    .collect(Collectors.toList()));

            btnMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "evolve_item");
            btn.setItemMeta(btnMeta);
        }
        inventory.setItem(22, btn);
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        int slot = event.getRawSlot();

        // 🛡️ REGLA OMEGA: Solo permitimos interacción libre con el inventario del jugador y el slot 13 (La bahía)
        if (event.getClickedInventory() == inventory && slot != 13) {
            event.setCancelled(true);
        }

        // Si hizo clic en el botón de evolucionar
        if (slot == 22 && event.getClickedInventory() == inventory) {
            procesarEvolucion();
        }
    }

    private void procesarEvolucion() {
        ItemStack item = inventory.getItem(13);
        if (item == null || !item.hasItemMeta()) {
            player.sendMessage(NexoColor.parse("&#FF5555[!] Inserta un activo válido en la bahía de procesamiento."));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        ItemMeta meta = item.getItemMeta();
        // Usando las llaves estáticas de ItemManager que tenías en tu Listener original
        boolean isWeapon = meta.getPersistentDataContainer().has(ItemManager.llaveWeaponId, PersistentDataType.STRING);
        boolean isTool = meta.getPersistentDataContainer().has(ItemManager.llaveHerramientaId, PersistentDataType.STRING);

        if (!isWeapon && !isTool) {
            player.sendMessage(NexoColor.parse("&#FF5555[!] Este activo no soporta la Evolución Cénit."));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        int nivelActual = meta.getPersistentDataContainer().getOrDefault(ItemManager.llaveNivelEvolucion, PersistentDataType.INTEGER, 1);

        // 🌟 LÍMITE ABSOLUTO DEL JUEGO
        if (nivelActual >= 60) {
            player.sendMessage(NexoColor.parse("&#FFAA00[!] El activo ya ha alcanzado su Cénit (Nivel Máximo 60)."));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
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
                } else {
                    String mat = item.getType().name();
                    if (mat.contains("PICKAXE")) { playerSkillLevel = aUser.getSkillLevel(Skills.MINING); skillName = "Minería"; }
                    else if (mat.contains("AXE")) { playerSkillLevel = aUser.getSkillLevel(Skills.FORAGING); skillName = "Tala"; }
                    else if (mat.contains("HOE")) { playerSkillLevel = aUser.getSkillLevel(Skills.FARMING); skillName = "Agricultura"; }
                    else if (mat.contains("SPADE") || mat.contains("SHOVEL")) { playerSkillLevel = aUser.getSkillLevel(Skills.EXCAVATION); skillName = "Excavación"; }
                }
            }
        } catch (Exception ignored) {}

        // Bloquear la evolución si el ítem superaría la maestría del jugador
        if (targetNivel > playerSkillLevel) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            player.sendMessage(NexoColor.parse("&#FF5555[!] Nivel de Operario Insuficiente. Requiere Nivel " + targetNivel + " en " + skillName + "."));
            return;
        }

        // Realizamos la mejora
        meta.getPersistentDataContainer().set(ItemManager.llaveNivelEvolucion, PersistentDataType.INTEGER, targetNivel);
        item.setItemMeta(meta);

        // Sincronizamos los stats del ítem
        ItemManager.sincronizarItemAsync(item);

        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1.5f);
        player.sendMessage(NexoColor.parse("&#55FF55[✓] Evolución completada. El activo ha ascendido al Nivel " + targetNivel + "."));
    }

    // 🛡️ SALVAVIDAS: Si el jugador cierra el menú con el ítem adentro, se lo devolvemos
    @Override
    public void handleClose(InventoryCloseEvent event) {
        ItemStack item = inventory.getItem(13);
        if (item != null && item.getType() != Material.AIR) {
            player.getInventory().addItem(item).values().forEach(
                    leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover)
            );
        }
    }
}