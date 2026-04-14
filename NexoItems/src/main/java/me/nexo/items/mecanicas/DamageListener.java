package me.nexo.items.mecanicas;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.NexoAPI;
import me.nexo.core.user.NexoUser;
import me.nexo.core.utils.NexoColor;
import me.nexo.items.NexoItems;
import me.nexo.items.dtos.ArmorDTO;
import me.nexo.items.dtos.EnchantDTO;
import me.nexo.items.dtos.WeaponDTO;
import me.nexo.items.managers.FileManager;
import me.nexo.items.managers.ItemManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.attribute.AttributeInstance;

/**
 * 🎒 NexoItems - Motor de Combate RPG (Arquitectura Enterprise - Alto Rendimiento)
 */
@Singleton
public class DamageListener implements Listener {

    private final NexoItems plugin;
    private final FileManager fileManager;

    // ⚡ CACHÉ DE RENDIMIENTO: Evita crear objetos nuevos en cada golpe
    private final NamespacedKey keyEvasion;
    private final NamespacedKey keyEspinosa;
    private final NamespacedKey keyEjecutor;
    private final NamespacedKey keyCazador;
    private final NamespacedKey keyVeneno;
    private final NamespacedKey keyVampirismo;

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public DamageListener(NexoItems plugin) {
        this.plugin = plugin;
        this.fileManager = plugin.getFileManager();

        // Inicializamos las llaves una sola vez al arrancar
        this.keyEvasion = new NamespacedKey(plugin, "nexo_enchant_evasion");
        this.keyEspinosa = new NamespacedKey(plugin, "nexo_enchant_coraza_espinosa");
        this.keyEjecutor = new NamespacedKey(plugin, "nexo_enchant_ejecutor");
        this.keyCazador = new NamespacedKey(plugin, "nexo_enchant_cazador");
        this.keyVeneno = new NamespacedKey(plugin, "nexo_enchant_veneno");
        this.keyVampirismo = new NamespacedKey(plugin, "nexo_enchant_vampirismo");
    }

    // Usamos prioridad HIGH para calcular el daño final después de los reducotres vanilla
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void alPegar(EntityDamageByEntityEvent event) {

        // ==========================================
        // 🛡️ LÓGICA DE DEFENSA (Si la víctima es un Jugador)
        // ==========================================
        if (event.getEntity() instanceof Player victima) {
            double probEvasion = 0.0;
            double reflejoEspinosa = 0.0;
            double defensaExtra = 0.0;

            // Escaneamos la armadura que lleva puesta
            for (ItemStack armor : victima.getInventory().getArmorContents()) {
                if (armor == null || !armor.hasItemMeta()) continue;
                var pdc = armor.getItemMeta().getPersistentDataContainer();

                // 1. Mitigación de daño basada en Tier/Vida de la armadura
                if (pdc.has(ItemManager.llaveArmaduraId, PersistentDataType.STRING)) {
                    ArmorDTO dto = fileManager.getArmorDTO(pdc.get(ItemManager.llaveArmaduraId, PersistentDataType.STRING));
                    if (dto != null) {
                        defensaExtra += (dto.vidaExtra() / 10.0);
                    }
                }

                // 2. Leer Encantamiento: Evasión
                if (pdc.has(keyEvasion, PersistentDataType.INTEGER)) {
                    EnchantDTO ench = fileManager.getEnchantDTO("evasion");
                    if (ench != null) probEvasion += ench.getValorPorNivel(pdc.get(keyEvasion, PersistentDataType.INTEGER));
                }

                // 3. Leer Encantamiento: Coraza Espinosa
                if (pdc.has(keyEspinosa, PersistentDataType.INTEGER)) {
                    EnchantDTO ench = fileManager.getEnchantDTO("coraza_espinosa");
                    if (ench != null) reflejoEspinosa += ench.getValorPorNivel(pdc.get(keyEspinosa, PersistentDataType.INTEGER));
                }
            }

            // Aplicar Evasión Mágica
            if (probEvasion > 0 && Math.random() * 100 <= probEvasion) {
                event.setCancelled(true);
                CrossplayUtils.sendMessage(victima, "&#00E5FF<bold>¡EVASIÓN PERFECTA!</bold>");
                victima.playSound(victima.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 2f);
                return;
            }

            // Aplicar Mitigación de Daño
            double danioReducido = Math.max(1.0, event.getDamage() - defensaExtra);
            event.setDamage(danioReducido);

            // Aplicar Coraza Espinosa
            if (reflejoEspinosa > 0 && event.getDamager() instanceof LivingEntity atacante) {
                double danioDevuelto = danioReducido * (reflejoEspinosa / 100.0);
                atacante.damage(danioDevuelto, victima);
            }
        }

        // ==========================================
        // ⚔️ LÓGICA DE ATAQUE (Si el atacante es un Jugador)
        // ==========================================
        if (event.getDamager() instanceof Player jugador && event.getEntity() instanceof LivingEntity target) {

            ItemStack arma = jugador.getInventory().getItemInMainHand();
            if (arma == null || !arma.hasItemMeta()) return;

            var pdc = arma.getItemMeta().getPersistentDataContainer();

            if (pdc.has(ItemManager.llaveWeaponId, PersistentDataType.STRING)) {
                String idArma = pdc.get(ItemManager.llaveWeaponId, PersistentDataType.STRING);
                WeaponDTO dto = fileManager.getWeaponDTO(idArma);

                if (dto != null) {
                    NexoUser user = NexoAPI.getInstance().getUserLocal(jugador.getUniqueId());

                    String claseJugador = "Ninguna";
                    int nivelCombate = 1;

                    if (user != null) {
                        claseJugador = user.getClaseJugador();
                        nivelCombate = user.getCombateNivel();
                    }

                    // 1. RESTRICCIONES (Clase y Nivel de Combate)
                    if (!dto.claseRequerida().equalsIgnoreCase("Cualquiera") && !dto.claseRequerida().equalsIgnoreCase(claseJugador)) {
                        CrossplayUtils.sendMessage(jugador, "&#FF5555[!] Incompatibilidad Neural: Tu clase (" + claseJugador + ") no puede empuñar este activo.");
                        jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                        event.setDamage(1.0); // Daño de castigo miserable
                        return;
                    }

                    if (nivelCombate < dto.nivelRequerido()) {
                        event.setCancelled(true);
                        CrossplayUtils.sendMessage(jugador, "&#FF5555[!] Activo Bloqueado. Requiere Autorización de Combate Nivel " + dto.nivelRequerido());
                        jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                        return;
                    }

                    // 2. DAÑO BASE
                    double dañoFinal = event.getDamage();

                    // 🌟 3. EVOLUCIÓN CÉNIT (Escalado de poder)
                    int nivelEvolucion = pdc.getOrDefault(ItemManager.llaveNivelEvolucion, PersistentDataType.INTEGER, 1);
                    double escaladoNivel = 1.0 + (nivelEvolucion * 0.05); // +5% Daño por Nivel
                    dañoFinal *= escaladoNivel;

                    // 4. MULTIPLICADOR DE PRESTIGIO
                    int prestigio = pdc.getOrDefault(ItemManager.llaveWeaponPrestige, PersistentDataType.INTEGER, 0);
                    if (prestigio > 0 && dto.permitePrestigio()) {
                        dañoFinal += (dañoFinal * (prestigio * dto.multiPrestigio()));
                    }

                    // ==========================================
                    // 🪄 LECTURA DE ENCANTAMIENTOS OFENSIVOS
                    // ==========================================

                    // -- Ejecutor --
                    if (pdc.has(keyEjecutor, PersistentDataType.INTEGER)) {
                        // 🌟 CORREGIDO: AttributeInstance en lugar de Attribute
                        AttributeInstance maxHealthAttr = target.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                        if (maxHealthAttr != null) {
                            double maxHp = maxHealthAttr.getValue();
                            if ((target.getHealth() / maxHp) <= 0.20) {
                                EnchantDTO ench = fileManager.getEnchantDTO("ejecutor");
                                if (ench != null) {
                                    double bono = ench.getValorPorNivel(pdc.get(keyEjecutor, PersistentDataType.INTEGER));
                                    dañoFinal += (dañoFinal * (bono / 100.0));
                                }
                            }
                        }
                    }

                    // -- Cazador --
                    if (pdc.has(keyCazador, PersistentDataType.INTEGER) && target instanceof Monster) {
                        EnchantDTO ench = fileManager.getEnchantDTO("cazador");
                        if (ench != null) {
                            double bono = ench.getValorPorNivel(pdc.get(keyCazador, PersistentDataType.INTEGER));
                            dañoFinal += (dañoFinal * (bono / 100.0));
                        }
                    }

                    // -- Veneno Mortal --
                    if (pdc.has(keyVeneno, PersistentDataType.INTEGER)) {
                        EnchantDTO ench = fileManager.getEnchantDTO("veneno");
                        if (ench != null) {
                            int duracionTicks = (int) (ench.getValorPorNivel(pdc.get(keyVeneno, PersistentDataType.INTEGER)) * 20);
                            target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, duracionTicks, 0, false, false, false));
                        }
                    }

                    // 5. DAÑO Y EFECTOS ELEMENTALES
                    String elementoLimpio = ChatColor.stripColor(LegacyComponentSerializer.legacySection().serialize(NexoColor.parse(dto.elemento()))).toUpperCase();
                    String nombreMob = target.getCustomName() != null ? ChatColor.stripColor(target.getCustomName()).toUpperCase() : "";
                    double multElemental = 1.0;

                    if (elementoLimpio.contains("FUEGO") || elementoLimpio.contains("MAGMA") || elementoLimpio.contains("SOLAR")) {
                        target.setFireTicks(60);
                        if (nombreMob.contains("[HIELO]")) multElemental = 2.0;
                    }
                    else if (elementoLimpio.contains("HIELO") || elementoLimpio.contains("AGUA")) {
                        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, false, false, false));
                        if (nombreMob.contains("[FUEGO]")) multElemental = 2.0;
                    }
                    else if (elementoLimpio.contains("RAYO") || elementoLimpio.contains("TORMENTA")) {
                        if (Math.random() <= 0.15) target.getWorld().strikeLightningEffect(target.getLocation());
                        if (nombreMob.contains("[AGUA]")) multElemental = 2.0;
                    }

                    dañoFinal *= multElemental;

                    if (multElemental > 1.0) {
                        CrossplayUtils.sendMessage(jugador, "&#55FF55<bold>¡GOLPE CRÍTICO ELEMENTAL!</bold>");
                    }

                    // APLICAR EL DAÑO FINAL
                    event.setDamage(dañoFinal);

                    // 6. VAMPIRISMO (Se calcula con el daño final real, después de todos los bonos)
                    if (pdc.has(keyVampirismo, PersistentDataType.INTEGER)) {
                        EnchantDTO ench = fileManager.getEnchantDTO("vampirismo");

                        // 🌟 CORREGIDO: AttributeInstance en lugar de Attribute
                        AttributeInstance playerMaxHealth = jugador.getAttribute(Attribute.GENERIC_MAX_HEALTH);

                        if (ench != null && playerMaxHealth != null) {
                            double porcentaje = ench.getValorPorNivel(pdc.get(keyVampirismo, PersistentDataType.INTEGER));
                            double cura = dañoFinal * (porcentaje / 100.0);
                            double maxVidaJugador = playerMaxHealth.getValue();

                            jugador.setHealth(Math.min(maxVidaJugador, jugador.getHealth() + cura));
                            jugador.getWorld().spawnParticle(org.bukkit.Particle.HEART, jugador.getLocation().add(0, 1, 0), 1);
                        }
                    }
                }
            }
        }
    }
}