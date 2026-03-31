package me.nexo.core;

import me.nexo.core.user.NexoAPI;
import me.nexo.core.user.NexoUser;
import me.nexo.core.utils.NexoColor;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.NamespacedKey;

import java.util.UUID;

public class HudTask extends BukkitRunnable {

    private final NexoCore plugin;
    private final NamespacedKey classKey;

    public HudTask(NexoCore plugin) {
        this.plugin = plugin;
        this.classKey = new NamespacedKey("nexoitems", "nexo_class");
    }

    @Override
    public void run() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.isOnline()) continue;

            UUID id = p.getUniqueId();
            NexoUser user = NexoAPI.getInstance().getUserLocal(id);

            // 1. LÓGICA DE BENDICIÓN (Booster Cookie)
            String voidIcon = "";
            if (user != null && user.isVoidBlessingActive()) {
                voidIcon = " &#ff00ff✧";
            }

            // 2. LÓGICA DE MANÁ (AuraSkills u otro)
            int manaActual = 0;
            int maxMana = 100;
            try {
                dev.aurelium.auraskills.api.user.SkillsUser userAura = dev.aurelium.auraskills.api.AuraSkillsApi.get().getUser(id);
                if (userAura != null) {
                    manaActual = (int) userAura.getMana();
                    maxMana = (int) userAura.getMaxMana();
                }
            } catch (Exception ignored) {}

            // Si tiene el Set de Inquisidor equipado, su maná máximo es x2 visualmente y funcionalmente
            if (hasFullSet(p, "INQUISITOR")) {
                maxMana *= 2; 
                // Aquí podrías sumar maná extra a manaActual si regenera rápido
            }

            // 3. BARRA DE MANÁ VISUAL [ ■■■■□ ]
            String manaBar = buildProgressBar(manaActual, maxMana, 5, "&#00f5ff■", "&#E6CCFF■");

            // 4. ESTADO DE CLASE O SKILL ACTIVA
            String activeFocus = "Ninguna";
            if (hasFullSet(p, "ASSASSIN")) activeFocus = "&#8b0000Asesino";
            else if (hasFullSet(p, "INQUISITOR")) activeFocus = "&#ff00ffInquisidor";
            else activeFocus = "&#E6CCFFAventurero";

            // 5. RENDERIZADO FINAL DEL ACTION BAR (Vivid Void Protocol)
            // Ejemplo: [ ■■■■□ ] 80/100 MP | Clase: Inquisidor ✧
            String hudFormat = String.format("%s &#00f5ff%d/%d MP &#E6CCFF| &#00f5ffClase: %s%s", 
                    manaBar, manaActual, maxMana, activeFocus, voidIcon);

            p.sendActionBar(NexoColor.parse(hudFormat));
        }
    }

    // 🌟 MOTOR DE RENDERIZADO DE BARRAS (Ultra ligero)
    private String buildProgressBar(int current, int max, int totalBars, String filledSymbol, String emptySymbol) {
        if (max <= 0) max = 1;
        float percent = (float) current / max;
        int progressBars = (int) (totalBars * percent);

        StringBuilder bar = new StringBuilder("&#E6CCFF[ ");
        for (int i = 0; i < totalBars; i++) {
            if (i < progressBars) bar.append(filledSymbol);
            else bar.append(emptySymbol);
        }
        bar.append(" &#E6CCFF]");
        return bar.toString();
    }

    // Validador rápido de Sets para el HUD
    private boolean hasFullSet(Player player, String targetClass) {
        if (player.getInventory().getHelmet() == null) return false;
        String helmClass = getClassTag(player.getInventory().getHelmet());
        if (helmClass == null || !helmClass.equalsIgnoreCase(targetClass)) return false;

        String chestClass = getClassTag(player.getInventory().getChestplate());
        String legsClass = getClassTag(player.getInventory().getLeggings());
        String bootsClass = getClassTag(player.getInventory().getBoots());

        return targetClass.equalsIgnoreCase(chestClass) && 
               targetClass.equalsIgnoreCase(legsClass) && 
               targetClass.equalsIgnoreCase(bootsClass);
    }

    private String getClassTag(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        if (item.getItemMeta().getPersistentDataContainer().has(classKey, PersistentDataType.STRING)) {
            return item.getItemMeta().getPersistentDataContainer().get(classKey, PersistentDataType.STRING);
        }
        return null;
    }
}