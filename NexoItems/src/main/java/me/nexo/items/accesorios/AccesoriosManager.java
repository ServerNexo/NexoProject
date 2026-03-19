package me.nexo.items.accesorios;

import me.nexo.core.NexoCore;
import me.nexo.items.NexoItems;
import me.nexo.core.utils.Base64Util;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class AccesoriosManager {

    // 🟢 ARQUITECTURA: Ahora usamos NexoItems para la llave y las tareas asíncronas
    private final NexoItems plugin;
    public final Map<String, AccessoryDTO> registro = new HashMap<>();
    public final NamespacedKey llaveAccesorio;

    // Almacena quién tiene el Corazón del Nexo activo para el Failsafe
    public final Set<UUID> usuariosCorazonNexo = new HashSet<>();

    public AccesoriosManager(NexoItems plugin) {
        this.plugin = plugin;
        this.llaveAccesorio = new NamespacedKey(plugin, "nexo_accesorio_id");
        cargarBaseDeDatosAccesorios();
    }

    // ==========================================
    // 🗃️ REGISTRO DE LOS 26 ACCESORIOS
    // ==========================================
    private void cargarBaseDeDatosAccesorios() {
        // Minería
        registro.put("guijarro_magnetico", new AccessoryDTO("guijarro_magnetico", AccessoryDTO.Familia.MINERIA, AccessoryDTO.Rareza.COMUN, AccessoryDTO.StatType.FUERZA, 2, "Atrae minerales ligeramente."));
        registro.put("amuleto_espeleologo", new AccessoryDTO("amuleto_espeleologo", AccessoryDTO.Familia.MINERIA, AccessoryDTO.Rareza.EPICO, AccessoryDTO.StatType.VELOCIDAD, 0.05, "Visión en la oscuridad pura."));
        registro.put("reliquia_nucleo", new AccessoryDTO("reliquia_nucleo", AccessoryDTO.Familia.MINERIA, AccessoryDTO.Rareza.MITICO, AccessoryDTO.StatType.VIDA, 20, "Inmunidad parcial a la lava."));
        // Tala
        registro.put("brote_magico", new AccessoryDTO("brote_magico", AccessoryDTO.Familia.TALA, AccessoryDTO.Rareza.COMUN, AccessoryDTO.StatType.ENERGIA_CUSTOM, 10, "Regenera energía en bosques."));
        registro.put("rama_viva", new AccessoryDTO("rama_viva", AccessoryDTO.Familia.TALA, AccessoryDTO.Rareza.RARO, AccessoryDTO.StatType.FUERZA, 5, "+Daño a mobs de madera."));
        registro.put("raiz_arbol_mundo", new AccessoryDTO("raiz_arbol_mundo", AccessoryDTO.Familia.TALA, AccessoryDTO.Rareza.LEGENDARIO, AccessoryDTO.StatType.VIDA, 40, "Resistencia extrema al empuje."));
        // Cosecha
        registro.put("herradura_oxidada", new AccessoryDTO("herradura_oxidada", AccessoryDTO.Familia.COSECHA, AccessoryDTO.Rareza.COMUN, AccessoryDTO.StatType.VELOCIDAD, 0.02, "Ligeramente más rápido en pasto."));
        registro.put("trebol_4_hojas", new AccessoryDTO("trebol_4_hojas", AccessoryDTO.Familia.COSECHA, AccessoryDTO.Rareza.RARO, AccessoryDTO.StatType.ENERGIA_CUSTOM, 20, "Suerte en drops de cultivos."));
        registro.put("bendicion_demeter", new AccessoryDTO("bendicion_demeter", AccessoryDTO.Familia.COSECHA, AccessoryDTO.Rareza.MITICO, AccessoryDTO.StatType.VIDA, 30, "Aura de crecimiento automático."));
        // Pesca
        registro.put("anzuelo_oxidado", new AccessoryDTO("anzuelo_oxidado", AccessoryDTO.Familia.PESCA, AccessoryDTO.Rareza.COMUN, AccessoryDTO.StatType.FUERZA, 1, "Poco útil."));
        registro.put("cebo_plata", new AccessoryDTO("cebo_plata", AccessoryDTO.Familia.PESCA, AccessoryDTO.Rareza.RARO, AccessoryDTO.StatType.VELOCIDAD, 0.03, "Atrae peces raros."));
        registro.put("esfera_leviatan", new AccessoryDTO("esfera_leviatan", AccessoryDTO.Familia.PESCA, AccessoryDTO.Rareza.LEGENDARIO, AccessoryDTO.StatType.ARMADURA, 8, "Respiración acuática infinita."));
        // Combate (Tanque, Melee, Rango)
        registro.put("escudo_roto", new AccessoryDTO("escudo_roto", AccessoryDTO.Familia.TANQUE, AccessoryDTO.Rareza.COMUN, AccessoryDTO.StatType.ARMADURA, 2, "Bloqueo básico."));
        registro.put("egida", new AccessoryDTO("egida", AccessoryDTO.Familia.TANQUE, AccessoryDTO.Rareza.EPICO, AccessoryDTO.StatType.VIDA, 25, "Absorbe 10% del daño."));
        registro.put("coraza_titan", new AccessoryDTO("coraza_titan", AccessoryDTO.Familia.TANQUE, AccessoryDTO.Rareza.MITICO, AccessoryDTO.StatType.ARMADURA, 15, "Inmune al primer golpe."));
        registro.put("garra", new AccessoryDTO("garra", AccessoryDTO.Familia.MELEE, AccessoryDTO.Rareza.RARO, AccessoryDTO.StatType.FUERZA, 8, "Ataques sangrantes."));
        registro.put("colmillo", new AccessoryDTO("colmillo", AccessoryDTO.Familia.MELEE, AccessoryDTO.Rareza.LEGENDARIO, AccessoryDTO.StatType.FUERZA, 18, "Robo de vida leve."));
        registro.put("pluma", new AccessoryDTO("pluma", AccessoryDTO.Familia.RANGO, AccessoryDTO.Rareza.RARO, AccessoryDTO.StatType.VELOCIDAD, 0.05, "Caída lenta."));
        registro.put("astrolabio", new AccessoryDTO("astrolabio", AccessoryDTO.Familia.RANGO, AccessoryDTO.Rareza.EPICO, AccessoryDTO.StatType.ENERGIA_CUSTOM, 30, "Precisión astral."));
        // Utilidad
        registro.put("prisma", new AccessoryDTO("prisma", AccessoryDTO.Familia.ENERGIA, AccessoryDTO.Rareza.EPICO, AccessoryDTO.StatType.ENERGIA_CUSTOM, 50, "-5% Costo de Energía."));
        registro.put("ojo_intelecto", new AccessoryDTO("ojo_intelecto", AccessoryDTO.Familia.ENERGIA, AccessoryDTO.Rareza.MITICO, AccessoryDTO.StatType.ENERGIA_CUSTOM, 100, "Regeneración de Maná x2."));
        registro.put("vendaje", new AccessoryDTO("vendaje", AccessoryDTO.Familia.MOVILIDAD, AccessoryDTO.Rareza.COMUN, AccessoryDTO.StatType.VIDA, 5, "Cura pasiva leve."));
        registro.put("espuela", new AccessoryDTO("espuela", AccessoryDTO.Familia.MOVILIDAD, AccessoryDTO.Rareza.EPICO, AccessoryDTO.StatType.VELOCIDAD, 0.10, "Doble salto permitido."));
        registro.put("sello", new AccessoryDTO("sello", AccessoryDTO.Familia.RIQUEZA, AccessoryDTO.Rareza.LEGENDARIO, AccessoryDTO.StatType.ARMADURA, 5, "+15% Monedas obtenidas."));
        registro.put("moneda", new AccessoryDTO("moneda", AccessoryDTO.Familia.RIQUEZA, AccessoryDTO.Rareza.COMUN, AccessoryDTO.StatType.FUERZA, 0, "+1% Monedas obtenidas."));
        registro.put("talisman", new AccessoryDTO("talisman", AccessoryDTO.Familia.CAZAJEFES, AccessoryDTO.Rareza.EPICO, AccessoryDTO.StatType.FUERZA, 10, "+10% Daño a Jefes."));
        // El Mito
        registro.put("corazon_nexo", new AccessoryDTO("corazon_nexo", AccessoryDTO.Familia.CAZAJEFES, AccessoryDTO.Rareza.COSMICO, AccessoryDTO.StatType.VIDA, 100, "Te revive una vez cada hora al recibir daño letal."));
    }

    // ==========================================
    // 🎒 LÓGICA DEL GUI Y SLOTS DESBLOQUEADOS
    // ==========================================
    public void abrirBolsa(Player p) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {

            // 🟢 ARQUITECTURA LIMPIA: Buscamos a NexoCore en el servidor para usar su base de datos
            NexoCore nexoCore = (NexoCore) Bukkit.getPluginManager().getPlugin("NexoCore");
            if (nexoCore == null || nexoCore.getDatabaseManager() == null) {
                p.sendMessage("§cError crítico: No se pudo contactar con la base de datos central.");
                return;
            }

            String sql = "SELECT contenido FROM nexo_storage WHERE uuid = ? AND tipo = 'accesorios'";
            String base64Data = null;

            try (Connection conn = nexoCore.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, p.getUniqueId().toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) base64Data = rs.getString("contenido");
            } catch (Exception e) { e.printStackTrace(); }

            String finalData = base64Data;
            Bukkit.getScheduler().runTask(plugin, () -> {
                // Cálculo de tamaño dinámico basado en Colección de Redstone
                int slotsDesbloqueados = getSlotsDesbloqueados(p);
                int filas = Math.max(1, (int) Math.ceil(slotsDesbloqueados / 9.0));

                Inventory inv = Bukkit.createInventory(null, filas * 9, "§8💍 Accessory Bag");

                if (finalData != null && !finalData.isEmpty()) {
                    inv.setContents(Base64Util.itemStackArrayFromBase64(finalData));
                }

                // Bloqueamos visualmente los slots no desbloqueados (Con cristal de Nexo/Vanilla)
                ItemStack cristalBloqueado = new ItemStack(Material.RED_STAINED_GLASS_PANE);
                ItemMeta meta = cristalBloqueado.getItemMeta();
                meta.setDisplayName("§c§l🔒 Slot Bloqueado");
                meta.setLore(List.of("§7Aumenta tu Colección de Redstone", "§7para desbloquear más espacio."));
                cristalBloqueado.setItemMeta(meta);

                for (int i = slotsDesbloqueados; i < inv.getSize(); i++) {
                    inv.setItem(i, cristalBloqueado);
                }

                p.openInventory(inv);
                p.playSound(p.getLocation(), org.bukkit.Sound.ITEM_ARMOR_EQUIP_GOLD, 1f, 1f);
            });
        });
    }

    // Hook Simulado de AuraCollections
    public int getSlotsDesbloqueados(Player p) {
        // Aquí conectarías con tu API de colecciones.
        // Ejemplo: return AuraCollectionsAPI.getCollectionLevel(p, "REDSTONE") + 3;
        // Por ahora, simulamos que tienen entre 9 y 18 slots.
        return 12; // 12 slots desbloqueados para probar
    }

    // Hook Simulado de Validación de Ítem
    public boolean cumpleRequisito(Player p, AccessoryDTO dto) {
        // Ejemplo: Si es la "reliquia_nucleo", requiere 100k de obsidiana.
        return true; // Asumimos que lo cumple por defecto
    }

    // ==========================================
    // ⚙️ PROCESAMIENTO MATEMÁTICO (No Stacking)
    // ==========================================
    public void procesarYGuardarBolsa(Player p, Inventory inv) {
        // 1. Guardar Asíncronamente en BD
        String base64Data = Base64Util.itemStackArrayToBase64(inv.getContents());
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {

            // 🟢 ARQUITECTURA LIMPIA: Igual que arriba, usamos el NexoCore para la escritura
            NexoCore nexoCore = (NexoCore) Bukkit.getPluginManager().getPlugin("NexoCore");
            if (nexoCore == null || nexoCore.getDatabaseManager() == null) return;

            String sql = "INSERT INTO nexo_storage (uuid, tipo, contenido) VALUES (?, 'accesorios', ?) " +
                    "ON CONFLICT (uuid, tipo) DO UPDATE SET contenido = EXCLUDED.contenido;";
            try (Connection conn = nexoCore.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, p.getUniqueId().toString());
                ps.setString(2, base64Data);
                ps.executeUpdate();
            } catch (Exception e) { e.printStackTrace(); }
        });

        // 2. Filtrado de Stacking por Familias
        Map<AccessoryDTO.Familia, AccessoryDTO> accesoriosActivos = new EnumMap<>(AccessoryDTO.Familia.class);

        for (ItemStack item : inv.getContents()) {
            if (item == null || item.getType() == Material.RED_STAINED_GLASS_PANE || !item.hasItemMeta()) continue;

            var pdc = item.getItemMeta().getPersistentDataContainer();
            if (pdc.has(llaveAccesorio, PersistentDataType.STRING)) {
                String id = pdc.get(llaveAccesorio, PersistentDataType.STRING);
                AccessoryDTO dto = registro.get(id);

                if (dto != null && cumpleRequisito(p, dto)) {
                    // Si ya hay uno de esta familia, nos quedamos con el de mayor rareza
                    if (accesoriosActivos.containsKey(dto.family())) {
                        AccessoryDTO existente = accesoriosActivos.get(dto.family());
                        if (dto.rarity().getPoderNexo() > existente.rarity().getPoderNexo()) {
                            accesoriosActivos.put(dto.family(), dto);
                        }
                    } else {
                        accesoriosActivos.put(dto.family(), dto);
                    }
                }
            }
        }

        // 3. Cálculo Final de Stats y Nexo Power
        Map<AccessoryDTO.StatType, Double> statsTotales = new EnumMap<>(AccessoryDTO.StatType.class);
        int poderTotal = 0;
        boolean corazon = false;

        for (AccessoryDTO activo : accesoriosActivos.values()) {
            poderTotal += activo.rarity().getPoderNexo();
            statsTotales.put(activo.statType(), statsTotales.getOrDefault(activo.statType(), 0.0) + activo.statValue());

            if (activo.id().equals("corazon_nexo")) corazon = true;
        }

        // Actualizamos estado de Corazón del Nexo globalmente
        if (corazon) usuariosCorazonNexo.add(p.getUniqueId());
        else usuariosCorazonNexo.remove(p.getUniqueId());

        // 4. Disparar Evento Bukkit
        AccessoryStatsUpdateEvent evento = new AccessoryStatsUpdateEvent(p, statsTotales, poderTotal, corazon);
        Bukkit.getPluginManager().callEvent(evento);
    }
}