package me.nexo.items.mecanicas;

import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.skill.Skills;
import me.nexo.core.utils.NexoColor;
import me.nexo.items.managers.ItemManager;
import me.nexo.items.NexoItems;
import me.nexo.items.dtos.ArmorDTO;
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
        double velocidadPescaTotal = 0.0;

        for (ItemStack item : p.getInventory().getArmorContents()) {
            if (item == null || !item.hasItemMeta()) continue;
            var pdc = item.getItemMeta().getPersistentDataContainer();

            if (pdc.has(ItemManager.llaveArmaduraId, PersistentDataType.STRING)) {
                ArmorDTO dto = plugin.getFileManager().getArmorDTO(pdc.get(ItemManager.llaveArmaduraId, PersistentDataType.STRING));
                if (dto != null) {
                    probCriaturaTotal += dto.criaturaMarina();
                    velocidadPescaTotal += dto.velocidadPesca();
                }
            }
        }

        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {

            if (random.nextDouble() * 100 <= probCriaturaTotal) {
                if (event.getCaught() != null) {
                    event.getCaught().remove();
                }
                spawnearMonstruoMarino(p);

                p.sendActionBar(NexoColor.parse("&#8b0000<bold>¡ALERTA DE SEGURIDAD! ENTIDAD ABISAL DETECTADA</bold> 🦑"));
                p.playSound(p.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.5f, 1.5f);
            } else {
                p.sendActionBar(NexoColor.parse("&#00f5ff✨ ¡EXTRACCIÓN ACUÁTICA EXITOSA! ✨"));
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
            p.getWorld().spawnEntity(p.getLocation(), EntityType.ZOMBIE).customName(NexoColor.parse("&#1c0f2aAhogado de las Mareas"));
        } else if (nivelPesca < 25) {
            p.getWorld().spawnEntity(p.getLocation(), EntityType.GUARDIAN).customName(NexoColor.parse("&#00f5ffGuardián de la Fosa"));
        } else {
            p.getWorld().spawnEntity(p.getLocation(), EntityType.ELDER_GUARDIAN).customName(NexoColor.parse("&#8b0000<bold>LEVIATÁN DEL NEXO</bold>"));
        }
    }
}