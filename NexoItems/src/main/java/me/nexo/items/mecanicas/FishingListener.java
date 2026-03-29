package me.nexo.items.mecanicas;

import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.skill.Skills;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.items.NexoItems;
import me.nexo.items.dtos.ArmorDTO;
import me.nexo.items.managers.ItemManager;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Random;

public class FishingListener implements Listener {

    private final NexoItems plugin;
    private final Random random = new Random();

    public FishingListener(NexoItems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void alPescar(PlayerFishEvent event) {
        Player p = event.getPlayer();

        double probCriaturaTotal = 0.0;
        for (ItemStack item : p.getInventory().getArmorContents()) {
            if (item == null || !item.hasItemMeta()) continue;
            var pdc = item.getItemMeta().getPersistentDataContainer();

            if (pdc.has(ItemManager.llaveArmaduraId, PersistentDataType.STRING)) {
                ArmorDTO dto = plugin.getFileManager().getArmorDTO(pdc.get(ItemManager.llaveArmaduraId, PersistentDataType.STRING));
                if (dto != null) {
                    probCriaturaTotal += dto.criaturaMarina();
                }
            }
        }

        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            if (random.nextDouble() * 100 <= probCriaturaTotal) {
                if (event.getCaught() != null) {
                    event.getCaught().remove();
                }
                spawnearMonstruoMarino(p);
                CrossplayUtils.sendActionBar(p, plugin.getConfigManager().getMessage("eventos.pesca.alerta-seguridad"));
                p.playSound(p.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.5f, 1.5f);
            } else {
                CrossplayUtils.sendActionBar(p, plugin.getConfigManager().getMessage("eventos.pesca.extraccion-exitosa"));
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 2.0f);
            }
        }
    }

    private void spawnearMonstruoMarino(Player p) {
        int nivelPesca = 1;
        try {
            nivelPesca = (int) AuraSkillsApi.get().getUser(p.getUniqueId()).getSkillLevel(Skills.FISHING);
        } catch (Exception ignored) {}

        if (nivelPesca < 10) {
            p.getWorld().spawnEntity(p.getLocation(), EntityType.ZOMBIE).customName(CrossplayUtils.parseCrossplay(p, plugin.getConfigManager().getMessage("eventos.pesca.monstruos.ahogado")));
        } else if (nivelPesca < 25) {
            p.getWorld().spawnEntity(p.getLocation(), EntityType.GUARDIAN).customName(CrossplayUtils.parseCrossplay(p, plugin.getConfigManager().getMessage("eventos.pesca.monstruos.guardian")));
        } else {
            p.getWorld().spawnEntity(p.getLocation(), EntityType.ELDER_GUARDIAN).customName(CrossplayUtils.parseCrossplay(p, plugin.getConfigManager().getMessage("eventos.pesca.monstruos.leviatan")));
        }
    }
}