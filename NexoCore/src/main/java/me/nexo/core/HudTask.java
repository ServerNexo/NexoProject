package me.nexo.core;

import me.nexo.core.user.NexoAPI;
import me.nexo.core.user.NexoUser;
import me.nexo.core.utils.NexoColor;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class HudTask extends BukkitRunnable {

    private final NexoCore plugin;

    public HudTask(NexoCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.isOnline()) continue;

            UUID id = p.getUniqueId();
            NexoUser user = NexoAPI.getInstance().getUserLocal(id);

            int maxEnergia = 100;
            int energiaActual = 100;
            
            // 🌟 VALIDACIÓN DE LA BENDICIÓN (Booster Cookie)
            String voidIcon = "";

            if (user != null) {
                int nivelNexo = user.getNexoNivel();
                maxEnergia = 100 + ((nivelNexo - 1) * 20) + user.getEnergiaExtraAccesorios();
                energiaActual = user.getEnergiaMineria();

                if (energiaActual < maxEnergia) {
                    int nuevaEnergia = Math.min(energiaActual + 5, maxEnergia);
                    user.setEnergiaMineria(nuevaEnergia);
                    energiaActual = nuevaEnergia;
                }
                
                // 🌟 Icono Magenta Eléctrico si está activo
                if (user.isVoidBlessingActive()) {
                    voidIcon = " &#ff00ff✧ ";
                }
            }

            int manaActual = 0;
            int maxMana = 0;
            try {
                dev.aurelium.auraskills.api.user.SkillsUser userAura = dev.aurelium.auraskills.api.AuraSkillsApi.get().getUser(id);
                if (userAura != null) {
                    manaActual = (int) userAura.getMana();
                    maxMana = (int) userAura.getMaxMana();
                }
            } catch (Exception ignored) {}

            int hpActual = (int) Math.ceil(p.getHealth());
            int hpMax = 20;
            if (p.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
                hpMax = (int) p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
            }

            // 🌟 NATIVO DE PAPER: Enviamos el HUD añadiendo el icono del vacío al final
            String hudFormat = "&#ff4b2b❤ " + hpActual + "/" + hpMax + "   &#00fbff💧 " + manaActual + "/" + maxMana + "   &#fbd72b⚡ " + energiaActual + "/" + maxEnergia + voidIcon;
            p.sendActionBar(NexoColor.parse(hudFormat));
        }
    }
}