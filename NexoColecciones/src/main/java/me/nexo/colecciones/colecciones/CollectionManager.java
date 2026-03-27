package me.nexo.colecciones.colecciones;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import me.nexo.colecciones.NexoColecciones;
import me.nexo.colecciones.data.CollectionCategory;
import me.nexo.colecciones.data.CollectionItem;
import me.nexo.colecciones.data.Tier;
import me.nexo.core.NexoCore;
import me.nexo.core.utils.NexoColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CollectionManager {

    private final NexoColecciones plugin;

    // 🎨 PALETA HEX - CONSTANTES DEL VACÍO
    private static final String BC_DIVIDER = "&#434343=======================================";
    private static final String MSG_LEVEL_UP_TITLE = "&#fbd72b<bold>⭐ ¡NUEVO NIVEL DE COLECCIÓN ALCANZADO!</bold>";
    private static final String MSG_LEVEL_UP_DESC = "&#e0e0e0Has alcanzado el Nivel %level% en: &#00fbff%item%";

    private static final String MSG_REWARD_CLAIMED = "&#55FF55[✓] <bold>RECOMPENSA RECLAMADA:</bold> &#e0e0e0El vacío te ha entregado su poder.";

    private static final String BC_MAX_LEVEL = "&#434343[&#fbd72b<bold>NEXO</bold>&#434343] &#00fbff¡El operario &#a8ff78%player% &#00fbffha dominado por completo la colección de &#fbd72b%item%&#00fbff!";

    // Mapeo en Memoria
    private Map<String, CollectionCategory> categoriasRegistradas = new HashMap<>();
    private final Map<UUID, CollectionProfile> perfilesJugadores = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();

    public CollectionManager(NexoColecciones plugin) {
        this.plugin = plugin;
    }

    // ==========================================================
    // ⚙️ MOTOR DE CARGA (Lee del Config y Base de Datos)
    // ==========================================================

    public void cargarDesdeConfig() {
        // Usa el nuevo motor de ensamblaje que creamos en ColeccionesConfig
        this.categoriasRegistradas = plugin.getColeccionesConfig().cargarCategoriasEnRam();
        plugin.getLogger().info("Se han cargado " + categoriasRegistradas.size() + " categorías de colecciones.");
    }

    public void loadPlayerFromDatabase(UUID uuid, DataSource dataSource) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // Actualiza tu SQL para leer también la columna 'claimed_tiers' (Asegúrate de crear esta columna en tu DB)
            String sql = "SELECT collections_data, claimed_tiers FROM nexo_collections WHERE uuid = ?";

            try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    String jsonProgress = rs.getString("collections_data");
                    String jsonClaimed = rs.getString("claimed_tiers");

                    Map<String, Integer> mapProgress = gson.fromJson(jsonProgress, new TypeToken<Map<String, Integer>>(){}.getType());
                    Map<String, Set<Integer>> mapClaimed = gson.fromJson(jsonClaimed, new TypeToken<Map<String, Set<Integer>>>(){}.getType());

                    if (mapProgress == null) mapProgress = new HashMap<>();
                    if (mapClaimed == null) mapClaimed = new HashMap<>();

                    perfilesJugadores.put(uuid, new CollectionProfile(uuid, mapProgress, mapClaimed));
                } else {
                    perfilesJugadores.put(uuid, new CollectionProfile(uuid, new HashMap<>(), new HashMap<>()));
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error cargando perfil de colección para: " + uuid.toString());
                perfilesJugadores.put(uuid, new CollectionProfile(uuid, new HashMap<>(), new HashMap<>()));
            }
        });
    }

    // ==========================================================
    // 🧮 MOTOR DE PROGRESO SILENCIOSO Y TÍTULOS
    // ==========================================================

    public void addProgress(Player player, String itemId, int amount) {
        itemId = itemId.toLowerCase(); // Normalizamos IDs
        CollectionItem item = getItemGlobal(itemId);
        if (item == null) return;

        CollectionProfile profile = perfilesJugadores.get(player.getUniqueId());
        if (profile == null) return;

        int nivelViejo = calcularNivel(item, profile.getProgress(itemId));
        profile.addProgress(itemId, amount);
        int nivelNuevo = calcularNivel(item, profile.getProgress(itemId));

        // Si subió de nivel, le avisamos con un TÍTULO EN PANTALLA y Mensajes (Bedrock Compatible)
        if (nivelNuevo > nivelViejo) {

            // 🌟 PARCHE A: Título masivo en pantalla (Usando LegacySection para colores seguros en Bedrock/Java)
            String titleStr = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(NexoColor.parse("&#fbd72b⭐ NIVEL " + nivelNuevo + " ⭐"));
            String subStr = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(NexoColor.parse("&#00fbff¡" + item.getNombre() + " subió de nivel!"));

            player.sendTitle(titleStr, subStr, 10, 70, 20);

            // Mensajes en el Chat
            player.sendMessage(NexoColor.parse(BC_DIVIDER));
            player.sendMessage(NexoColor.parse(MSG_LEVEL_UP_TITLE));
            player.sendMessage(NexoColor.parse(MSG_LEVEL_UP_DESC.replace("%level%", String.valueOf(nivelNuevo)).replace("%item%", item.getNombre())));
            player.sendMessage(NexoColor.parse(BC_DIVIDER));

            // Sonido Épico
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);

            // Anuncio global si alcanzó el nivel máximo
            if (nivelNuevo == item.getMaxTier()) {
                Bukkit.broadcast(NexoColor.parse(" "));
                Bukkit.broadcast(NexoColor.parse(BC_MAX_LEVEL.replace("%player%", player.getName()).replace("%item%", item.getNombre())));
                Bukkit.broadcast(NexoColor.parse(" "));
            }
        }
    }

    public int calcularNivel(CollectionItem item, int cantidadFarmeada) {
        int nivelAlcanzado = 0;
        // Ordenamos los tiers de menor a mayor
        List<Integer> niveles = new ArrayList<>(item.getTiers().keySet());
        Collections.sort(niveles);

        for (int nivel : niveles) {
            Tier tier = item.getTier(nivel);
            if (cantidadFarmeada >= tier.getRequerido()) {
                nivelAlcanzado = nivel;
            } else {
                break;
            }
        }
        return nivelAlcanzado;
    }

    // ==========================================================
    // 🎁 MOTOR DE RECOMPENSAS MANUAL
    // ==========================================================

    public void reclamarRecompensa(Player player, String itemId, int targetTier) {
        CollectionProfile profile = perfilesJugadores.get(player.getUniqueId());
        if (profile == null) return;

        CollectionItem item = getItemGlobal(itemId);
        if (item == null) return;

        Tier tier = item.getTier(targetTier);
        if (tier == null) return;

        // Verificar que tiene suficiente progreso
        if (profile.getProgress(itemId) < tier.getRequerido()) return;

        // Verificar que no lo haya reclamado ya
        if (profile.hasClaimedTier(itemId, targetTier)) return;

        // Entregar la recompensa
        ejecutarRecompensas(player, tier.getRecompensas());

        // Marcar como reclamado
        profile.markTierAsClaimed(itemId, targetTier);

        // Efectos épicos (Dopamina)
        player.sendMessage(NexoColor.parse(MSG_REWARD_CLAIMED));
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        player.getWorld().spawnParticle(org.bukkit.Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 100, 0.5, 0.5, 0.5, 0.1);
    }

    private void ejecutarRecompensas(Player player, List<String> acciones) {
        for (String accion : acciones) {
            String pName = player.getName();

            if (accion.startsWith("[comando] ")) {
                String cmd = accion.replace("[comando] ", "").replace("{player}", pName).trim();
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);

            } else if (accion.startsWith("[permiso] ")) {
                String perm = accion.replace("[permiso] ", "").replace("{player}", pName).trim();
                // Otorga el permiso a través del sistema base de Bukkit (Temporal) o ejecuta comando LuckPerms
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + pName + " permission set " + perm + " true");
            }
        }
    }

    // ==========================================================
    // 🏆 SISTEMA DE TOP GLOBAL (SUPABASE)
    // ==========================================================

    public void calcularTopAsync(Player player, String itemId) {
        CollectionItem cItem = getItemGlobal(itemId);

        if (cItem == null) {
            player.sendMessage(NexoColor.parse("&#ff4b2b[!] Error: Esa colección no existe en la base de datos."));
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // Consulta SQL adaptada para leer de tu JSONB
            String sql = "SELECT j.name, CAST(c.collections_data->>? AS INTEGER) as amount " +
                    "FROM nexo_collections c " +
                    "JOIN jugadores j ON c.uuid = j.uuid " +
                    "WHERE c.collections_data ? ? " +
                    "ORDER BY amount DESC LIMIT 5";

            try (Connection conn = NexoCore.getPlugin(NexoCore.class).getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                // Usamos el ID exacto que se guarda en el JSON
                ps.setString(1, cItem.getId());
                ps.setString(2, cItem.getId());
                ResultSet rs = ps.executeQuery();

                List<net.kyori.adventure.text.Component> lineasTop = new ArrayList<>();
                int rank = 1;
                while (rs.next()) {
                    String pName = rs.getString("name");
                    int amt = rs.getInt("amount");
                    lineasTop.add(NexoColor.parse("&#fbd72b" + rank + ". &#a8ff78" + pName + " &#434343- &#00fbff" + amt + " &#e0e0e0farmeados"));
                    rank++;
                }

                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(NexoColor.parse(BC_DIVIDER));
                    player.sendMessage(NexoColor.parse("&#fbd72b<bold>🏆 TOP 5 GLOBAL: </bold>" + cItem.getNombre()));

                    if (lineasTop.isEmpty()) {
                        player.sendMessage(NexoColor.parse("&#434343Nadie ha farmeado este recurso todavía..."));
                    } else {
                        for (net.kyori.adventure.text.Component l : lineasTop) {
                            player.sendMessage(l);
                        }
                    }
                    player.sendMessage(NexoColor.parse(BC_DIVIDER));
                });

            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(NexoColor.parse("&#ff4b2b[!] Error crítico de red al contactar con la base de datos.")));
            }
        });
    }

    // ==========================================================
    // 🔍 UTILIDADES Y GETTERS
    // ==========================================================

    public CollectionItem getItemGlobal(String itemId) {
        for (CollectionCategory cat : categoriasRegistradas.values()) {
            if (cat.getItems().containsKey(itemId.toLowerCase())) {
                return cat.getItems().get(itemId.toLowerCase());
            }
        }
        return null;
    }

    public Map<String, CollectionCategory> getCategorias() { return categoriasRegistradas; }
    public CollectionProfile getProfile(UUID uuid) { return perfilesJugadores.get(uuid); }
    public void removeProfile(UUID uuid) { perfilesJugadores.remove(uuid); }
    public Map<UUID, CollectionProfile> getPerfiles() { return perfilesJugadores; }

}