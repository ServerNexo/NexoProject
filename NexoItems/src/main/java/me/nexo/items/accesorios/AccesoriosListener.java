package me.nexo.items.accesorios;

// 🟢 Importamos el nuevo jefe
import me.nexo.items.NexoItems;
import me.nexo.core.user.NexoAPI;
import me.nexo.core.user.NexoUser;
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

    // 🟢 ARQUITECTURA: Ahora usamos NexoItems
    private final NexoItems plugin;
    private final AccesoriosManager manager;
    private final String TITULO_BOLSA = "§8💍 Accessory Bag";

    // Cooldown del Corazón del Nexo (1 Hora)
    private final Map<UUID, Long> cooldownCorazon = new ConcurrentHashMap<>();

    // Llaves únicas para los modificadores de atributos (Seguridad Anti-Infinito)
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

    // ==========================================
    // 🔒 PROTECCIÓN Y GUARDADO DEL INVENTARIO
    // ==========================================
    @EventHandler
    public void alCerrarBolsa(InventoryCloseEvent event) {
        if (event.getView().getTitle().equals(TITULO_BOLSA)) {
            manager.procesarYGuardarBolsa((Player) event.getPlayer(), event.getInventory());
            ((Player) event.getPlayer()).playSound(event.getPlayer().getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1.2f);
        }
    }

    @EventHandler
    public void alHacerClic(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(TITULO_BOLSA)) {
            ItemStack currentItem = event.getCurrentItem();

            // PROTECCIÓN 1: Evitar clics directos sobre los cristales de bloqueo
            if (currentItem != null && currentItem.getType() == Material.RED_STAINED_GLASS_PANE) {
                event.setCancelled(true);
                return;
            }

            // PROTECCIÓN 2: Evitar trucos con teclas de números (Hotbar Swap) hacia slots bloqueados
            if (event.getClick().name().contains("NUMBER_KEY")) {
                int slotDestino = event.getRawSlot();
                // Si están apuntando al inventario de arriba y está bloqueado
                if (slotDestino < event.getView().getTopInventory().getSize()) {
                    ItemStack slotItem = event.getView().getTopInventory().getItem(slotDestino);
                    if (slotItem != null && slotItem.getType() == Material.RED_STAINED_GLASS_PANE) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }

            // PROTECCIÓN 3: Evitar Shift-Clicks extraños que metan cristales
            if (event.isShiftClick() && currentItem != null && currentItem.getType() == Material.RED_STAINED_GLASS_PANE) {
                event.setCancelled(true);
            }
        }
    }

    // ==========================================
    // ⚙️ APLICACIÓN DE STATS GLOBALES (API 1.21.11)
    // ==========================================
    @EventHandler
    public void alActualizarStats(AccessoryStatsUpdateEvent event) {
        Player p = event.getPlayer();
        Map<AccessoryDTO.StatType, Double> stats = event.getStats();

        // 1. Aplicamos Atributos Nativos de Bukkit
        aplicarAtributo(p, Attribute.MAX_HEALTH, keyVida, stats.getOrDefault(AccessoryDTO.StatType.VIDA, 0.0));
        aplicarAtributo(p, Attribute.ATTACK_DAMAGE, keyFuerza, stats.getOrDefault(AccessoryDTO.StatType.FUERZA, 0.0));
        aplicarAtributo(p, Attribute.MOVEMENT_SPEED, keyVelocidad, stats.getOrDefault(AccessoryDTO.StatType.VELOCIDAD, 0.0));
        aplicarAtributo(p, Attribute.ARMOR, keyArmadura, stats.getOrDefault(AccessoryDTO.StatType.ARMADURA, 0.0));

        // 2. 🟢 ARQUITECTURA LIMPIA: Guardamos la energía extra en el NexoUser
        int energiaExtra = stats.getOrDefault(AccessoryDTO.StatType.ENERGIA_CUSTOM, 0.0).intValue();
        NexoUser user = NexoAPI.getInstance().getUserLocal(p.getUniqueId());
        if (user != null) {
            user.setEnergiaExtraAccesorios(energiaExtra);
        }

        p.sendMessage("§b✨ El Poder del Nexo de tus accesorios resuena: §l" + event.getNexoPower() + " PX");
    }

    private void aplicarAtributo(Player p, Attribute atributo, NamespacedKey key, double valor) {
        AttributeInstance instancia = p.getAttribute(atributo);
        if (instancia == null) return;

        // Limpieza de modificadores anteriores para evitar bug de stats infinitos
        for (AttributeModifier mod : instancia.getModifiers()) {
            if (mod.getKey().equals(key)) {
                instancia.removeModifier(mod);
            }
        }

        // Si hay un valor que agregar, lo añadimos
        if (valor > 0) {
            AttributeModifier modificador = new AttributeModifier(key, valor, AttributeModifier.Operation.ADD_NUMBER);
            instancia.addModifier(modificador);
        }
    }

    // ==========================================
    // 💖 MECÁNICA MÍTICA: EL CORAZÓN DEL NEXO
    // ==========================================
    @EventHandler
    public void alMorir(EntityResurrectEvent event) {
        if (event.getEntity() instanceof Player p) {
            // Si el evento está cancelado, significa que el jugador va a morir (no tiene Totem en la mano)
            if (event.isCancelled() && manager.usuariosCorazonNexo.contains(p.getUniqueId())) {

                long ahora = System.currentTimeMillis();
                long cooldownMilis = 3600 * 1000L; // 1 Hora en milisegundos

                if (!cooldownCorazon.containsKey(p.getUniqueId()) || (ahora - cooldownCorazon.get(p.getUniqueId())) > cooldownMilis) {

                    // ¡REVIVIR!
                    event.setCancelled(false);
                    cooldownCorazon.put(p.getUniqueId(), ahora);

                    p.setHealth(p.getAttribute(Attribute.MAX_HEALTH).getValue() * 0.5); // Revive con 50% HP
                    p.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, p.getLocation(), 150);
                    p.playSound(p.getLocation(), Sound.ITEM_TOTEM_USE, 1f, 0.5f);
                    p.sendTitle("§b§lMILAGRO CÓSMICO", "§7El Corazón del Nexo te ha salvado", 10, 60, 10);
                }
            }
        }
    }
}