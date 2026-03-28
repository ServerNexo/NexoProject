package me.nexo.mechanics.minigames;

import me.nexo.core.utils.NexoColor;
import me.nexo.mechanics.NexoMechanics;
import me.nexo.core.user.NexoAPI;
import me.nexo.core.user.NexoUser;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

public class FishingHookManager implements Listener {

    private final NexoMechanics plugin;

    public FishingHookManager(NexoMechanics plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void alPescar(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {

            if (event.getCaught() instanceof Item itemCapturado) {
                ItemStack pescado = itemCapturado.getItemStack();
                if (pescado == null || !pescado.hasItemMeta()) return;

                var pdc = pescado.getItemMeta().getPersistentDataContainer();

                boolean esPezCustom = false;
                for (NamespacedKey key : pdc.getKeys()) {
                    if (key.getNamespace().equalsIgnoreCase("evenmorefish") || key.getKey().contains("emf")) {
                        esPezCustom = true;
                        break;
                    }
                }

                if (esPezCustom) {
                    Player p = event.getPlayer();
                    NexoUser user = NexoAPI.getInstance().getUserLocal(p.getUniqueId());

                    if (user != null) {
                        int energiaAct = user.getEnergiaMineria();
                        int maxEnergia = 100 + ((user.getNexoNivel() - 1) * 20) + user.getEnergiaExtraAccesorios();

                        user.setEnergiaMineria(Math.min(energiaAct + 5, maxEnergia));
                        p.sendMessage(NexoColor.parse("&#00f5ff[✓] <bold>EXTRACCIÓN ACUÁTICA:</bold> &#1c0f2aReservas recargadas &#ff00ff(+5⚡)"));
                    }
                }
            }
        }
    }
}