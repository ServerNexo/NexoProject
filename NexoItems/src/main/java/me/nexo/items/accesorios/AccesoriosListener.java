package me.nexo.items.accesorios;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.items.NexoItems;
import me.nexo.core.user.NexoAPI;
import me.nexo.core.user.NexoUser;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AccesoriosListener implements Listener {

    private final NexoItems plugin;
    private final AccesoriosManager manager;

    private final Map<UUID, Long> cooldownCorazon = new ConcurrentHashMap<>();

    private final NamespacedKey keyVida;
    private final NamespacedKey keyFuerza;
    private final NamespacedKey keyVelocidad;
    private final NamespacedKey keyArmadura;

    public AccesoriosListener(NexoItems plugin, AccesoriosManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.keyVida = new NamespacedKey(plugin, "accesorio_vida");
        this.keyFuerza = new NamespacedKey(plugin, "accesorio_fuerza");
        this.keyVelocidad = new NamespacedKey(plugin, "accesorio_velocidad");
        this.keyArmadura = new NamespacedKey(plugin, "accesorio_armadura");
    }

    @EventHandler
    public void alCerrarBolsa(InventoryCloseEvent event) {
        String tituloLimpio = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (tituloLimpio.equals(plugin.getConfigManager().getMessage("menus.accesorios.titulo").replaceAll("<[^>]*>", ""))) {
            manager.procesarYGuardarBolsa((Player) event.getPlayer(), event.getInventory());
            ((Player) event.getPlayer()).playSound(event.getPlayer().getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1.2f);
        }
    }

    @EventHandler
    public void alHacerClic(InventoryClickEvent event) {
        String tituloLimpio = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (tituloLimpio.equals(plugin.getConfigManager().getMessage("menus.accesorios.titulo").replaceAll("<[^>]*>", ""))) {
            ItemStack currentItem = event.getCurrentItem();

            if (currentItem != null && currentItem.getType() == Material.RED_STAINED_GLASS_PANE) {
                event.setCancelled(true);
                return;
            }

            if (event.getClick().name().contains("NUMBER_KEY")) {
                int slotDestino = event.getRawSlot();
                if (slotDestino < event.getView().getTopInventory().getSize()) {
                    ItemStack slotItem = event.getView().getTopInventory().getItem(slotDestino);
                    if (slotItem != null && slotItem.getType() == Material.RED_STAINED_GLASS_PANE) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }

            if (event.isShiftClick() && currentItem != null && currentItem.getType() == Material.RED_STAINED_GLASS_PANE) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void alActualizarStats(AccessoryStatsUpdateEvent event) {
        Player p = event.getPlayer();
        Map<AccessoryDTO.StatType, Double> stats = event.getStats();

        aplicarAtributo(p, Attribute.GENERIC_MAX_HEALTH, keyVida, stats.getOrDefault(AccessoryDTO.StatType.VIDA, 0.0));
        aplicarAtributo(p, Attribute.GENERIC_ATTACK_DAMAGE, keyFuerza, stats.getOrDefault(AccessoryDTO.StatType.FUERZA, 0.0));
        aplicarAtributo(p, Attribute.GENERIC_MOVEMENT_SPEED, keyVelocidad, stats.getOrDefault(AccessoryDTO.StatType.VELOCIDAD, 0.0));
        aplicarAtributo(p, Attribute.GENERIC_ARMOR, keyArmadura, stats.getOrDefault(AccessoryDTO.StatType.ARMADURA, 0.0));

        int energiaExtra = stats.getOrDefault(AccessoryDTO.StatType.ENERGIA_CUSTOM, 0.0).intValue();
        NexoUser user = NexoAPI.getInstance().getUserLocal(p.getUniqueId());
        if (user != null) {
            user.setEnergiaExtraAccesorios(energiaExtra);
        }

        CrossplayUtils.sendMessage(p, plugin.getConfigManager().getMessage("eventos.accesorios.stats-actualizadas").replace("%power%", String.valueOf(event.getNexoPower())));
    }

    private void aplicarAtributo(Player p, Attribute atributo, NamespacedKey key, double valor) {
        AttributeInstance instancia = p.getAttribute(atributo);
        if (instancia == null) return;

        for (AttributeModifier mod : instancia.getModifiers()) {
            if (mod.getKey().equals(key)) {
                instancia.removeModifier(mod);
            }
        }

        if (valor > 0) {
            AttributeModifier modificador = new AttributeModifier(key, valor, AttributeModifier.Operation.ADD_NUMBER);
            instancia.addModifier(modificador);
        }
    }

    @EventHandler
    public void alMorir(EntityResurrectEvent event) {
        if (event.getEntity() instanceof Player p) {
            if (event.isCancelled() && manager.usuariosCorazonNexo.contains(p.getUniqueId())) {

                long ahora = System.currentTimeMillis();
                long cooldownMilis = 3600 * 1000L;

                if (!cooldownCorazon.containsKey(p.getUniqueId()) || (ahora - cooldownCorazon.get(p.getUniqueId())) > cooldownMilis) {

                    event.setCancelled(false);
                    cooldownCorazon.put(p.getUniqueId(), ahora);

                    p.setHealth(p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() * 0.5);
                    p.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, p.getLocation(), 150);
                    p.playSound(p.getLocation(), Sound.ITEM_TOTEM_USE, 1f, 0.5f);

                    CrossplayUtils.sendTitle(p,
                            plugin.getConfigManager().getMessage("eventos.accesorios.milagro.titulo"),
                            plugin.getConfigManager().getMessage("eventos.accesorios.milagro.subtitulo")
                    );
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        cooldownCorazon.remove(event.getPlayer().getUniqueId());
    }
}