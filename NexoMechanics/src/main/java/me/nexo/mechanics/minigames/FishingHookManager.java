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

    // 🟢 ARQUITECTURA: Usamos el plugin de Mechanics
    private final NexoMechanics plugin;

    public FishingHookManager(NexoMechanics plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void alPescar(PlayerFishEvent event) {
        // Solo nos interesa cuando el jugador tira exitosamente de la caña y saca algo
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {

            if (event.getCaught() instanceof Item itemCapturado) {
                ItemStack pescado = itemCapturado.getItemStack();
                if (pescado == null || !pescado.hasItemMeta()) return;

                // EvenMoreFish inyecta claves PDC en sus pescados (ej: emf-fish-name o emf-fish-rarity)
                var pdc = pescado.getItemMeta().getPersistentDataContainer();

                // Iteramos por las keys de EMF de forma segura (sin depender de su API directa)
                boolean esPezCustom = false;
                for (NamespacedKey key : pdc.getKeys()) {
                    if (key.getNamespace().equalsIgnoreCase("evenmorefish") || key.getKey().contains("emf")) {
                        esPezCustom = true;
                        break;
                    }
                }

                if (esPezCustom) {
                    Player p = event.getPlayer();

                    // 🟢 ARQUITECTURA LIMPIA: Conectamos con el ecosistema de Energía a través del NexoUser
                    NexoUser user = NexoAPI.getInstance().getUserLocal(p.getUniqueId());

                    if (user != null) {
                        int energiaAct = user.getEnergiaMineria();
                        // Calculamos la energía máxima incluyendo el nivel y los accesorios
                        int maxEnergia = 100 + ((user.getNexoNivel() - 1) * 20) + user.getEnergiaExtraAccesorios();

                        user.setEnergiaMineria(Math.min(energiaAct + 5, maxEnergia));
                        p.sendMessage(NexoColor.parse("&#00E5FF[✓] <bold>EXTRACCIÓN ACUÁTICA:</bold> &#AAAAAAReservas recargadas &#FFAA00(+5⚡)"));
                    }
                }
            }
        }
    }

    /*
     * 💡 Hook Opcional de MythicMobs (Bosses de Pesca).
     * Si instalas MythicMobs, puedes descomentar esto importando su API:
     * * @EventHandler
     * public void alMatarBoss(io.lumine.mythic.bukkit.events.MythicMobDeathEvent event) {
     * if (event.getMob().getType().getInternalName().contains("FISHING_BOSS")) {
     * if (event.getKiller() instanceof Player p) {
     * // Dar 50 Energía a los participantes
     * }
     * }
     * }
     */
}