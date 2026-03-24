package me.nexo.colecciones.colecciones;

import me.nexo.colecciones.NexoColecciones;
import me.nexo.colecciones.data.CollectionCategory;
import me.nexo.colecciones.data.CollectionItem;
import me.nexo.core.NexoCore;
import me.nexo.core.utils.NexoColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CollectionManager {

    private final NexoColecciones plugin;

    // 🎨 PALETA HEX - CONSTANTES (Clean Code)
    private static final String BC_DIVIDER = "&#434343=======================================";
    private static final String MSG_LEVEL_UP_TITLE = "&#fbd72b<bold>⭐ ¡NUEVO NIVEL DE COLECCIÓN ALCANZADO!</bold>";
    private static final String MSG_LEVEL_UP_DESC = "&#e0e0e0Has desbloqueado nuevas recompensas en: &#00fbff%item%";

    private static final String BC_MAX_LEVEL = "&#434343[&#fbd72b<bold>NEXO</bold>&#434343] &#00fbff¡El operario &#a8ff78%player% &#00fbffha alcanzado el &#fbd72b<bold>NIVEL MÁXIMO (%max%)</bold> &#00fbffen la colección de &#fbd72b%item%&#00fbff!";

    private static final String ERR_COLLECTION_404 = "&#ff4b2b[!] Error: Esa colección no existe en la base de datos.";
    private static final String MSG_TOP_TITLE = "&#fbd72b<bold>🏆 TOP 5 GLOBAL: %item%</bold>";
    private static final String MSG_TOP_EMPTY = "&#434343Nadie ha farmeado este recurso todavía...";
    private static final String ERR_TOP_LOAD = "&#ff4b2b[!] Error crítico de red al contactar con la base de datos.";
    private static final String MSG_TOP_FORMAT = "&#fbd72b%rank%. &#a8ff78%player% &#434343- &#00fbff%amount% &#e0e0e0farmeados";

    // 📊 TABLA DE PROGRESO GLOBAL (Requisitos de ítems para cada nivel)
    public static final int[] TIERS = {
            50, 100, 250, 500, 1000, 2500, 5000, 10000, 25000, 50000, 100000
    };

    private final Map<String, CollectionItem> itemsRegistrados = new ConcurrentHashMap<>();
    private final Map<UUID, CollectionProfile> perfilesJugadores = new ConcurrentHashMap<>();

    public CollectionManager(NexoColecciones plugin) {
        this.plugin = plugin;
    }

    public void cargarDesdeConfig() {
        itemsRegistrados.clear();
        org.bukkit.configuration.file.FileConfiguration config = plugin.getColeccionesConfig().getConfig();
        if (config == null || !config.contains("colecciones")) return;

        org.bukkit.configuration.ConfigurationSection seccionPrincipal = config.getConfigurationSection("colecciones");
        if (seccionPrincipal == null) return;

        for (String categoriaStr : seccionPrincipal.getKeys(false)) {
            CollectionCategory categoria;
            try {
                categoria = CollectionCategory.valueOf(categoriaStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                continue;
            }

            org.bukkit.configuration.ConfigurationSection seccionCategoria = seccionPrincipal.getConfigurationSection(categoriaStr);
            if (seccionCategoria == null) continue;

            for (String itemId : seccionCategoria.getKeys(false)) {
                org.bukkit.configuration.ConfigurationSection itemData = seccionCategoria.getConfigurationSection(itemId);
                if (itemData == null) continue;

                // Leemos el nombre. Dejamos los códigos HEX o de color nativos.
                String nombreBonito = itemData.getString("nombre", itemId);
                Map<Integer, List<String>> comandos = new HashMap<>();

                if (itemData.contains("recompensas")) {
                    org.bukkit.configuration.ConfigurationSection recompensas = itemData.getConfigurationSection("recompensas");
                    if (recompensas != null) {
                        for (String nivelStr : recompensas.getKeys(false)) {
                            try {
                                comandos.put(Integer.parseInt(nivelStr), recompensas.getStringList(nivelStr));
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }

                // 🌟 AQUÍ ESTÁ LA CORRECCIÓN DE LA LÍNEA ROJA (Orden y variables exactas)
                itemsRegistrados.put(itemId.toUpperCase(), new CollectionItem(itemId.toUpperCase(), categoria, nombreBonito, comandos));
            }
        }
    }

    public void loadPlayerFromDatabase(UUID uuid, DataSource dataSource) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "SELECT collections_data FROM nexo_collections WHERE uuid = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    String json = rs.getString("collections_data");
                    Map<String, Integer> map = new com.google.gson.Gson().fromJson(json, new com.google.gson.reflect.TypeToken<Map<String, Integer>>(){}.getType());
                    if (map == null) map = new HashMap<>();

                    // 🌟 SOLUCIÓN: Convertimos el Map genérico a ConcurrentHashMap
                    perfilesJugadores.put(uuid, new CollectionProfile(uuid, new java.util.concurrent.ConcurrentHashMap<>(map)));
                } else {
                    // 🌟 SOLUCIÓN: Usamos ConcurrentHashMap vacío
                    perfilesJugadores.put(uuid, new CollectionProfile(uuid, new java.util.concurrent.ConcurrentHashMap<>()));
                }
            } catch (Exception e) {
                // 🌟 SOLUCIÓN: Usamos ConcurrentHashMap vacío en el catch
                perfilesJugadores.put(uuid, new CollectionProfile(uuid, new java.util.concurrent.ConcurrentHashMap<>()));
            }
        });
    }

    public CollectionProfile getProfile(UUID uuid) {
        return perfilesJugadores.get(uuid);
    }
    public void removeProfile(UUID uuid) {
        perfilesJugadores.remove(uuid);
    }

    // Método para que el FlushTask pueda leer y guardar todos los perfiles
    public Map<UUID, CollectionProfile> getPerfiles() {
        return perfilesJugadores;
    }

    public Map<String, CollectionItem> getItemsRegistrados() {
        return itemsRegistrados;
    }

    public int calcularNivel(int cantidadFarmeada) {
        int nivel = 0;
        for (int requisto : TIERS) {
            if (cantidadFarmeada >= requisto) nivel++;
            else break;
        }
        return nivel;
    }

    public void addProgress(Player player, String itemId, int amount) {
        if (!itemsRegistrados.containsKey(itemId)) return;

        CollectionProfile profile = perfilesJugadores.get(player.getUniqueId());
        if (profile == null) return;

        int nivelViejo = calcularNivel(profile.getProgress(itemId));

        // 🌟 SOLUCIÓN: Agregamos 'false' porque esto es una colección, no un Slayer
        profile.addProgress(itemId, amount, false);

        int nivelNuevo = calcularNivel(profile.getProgress(itemId));

        if (nivelNuevo > nivelViejo) {
            CollectionItem item = itemsRegistrados.get(itemId);
            player.sendMessage(NexoColor.parse(BC_DIVIDER));
            player.sendMessage(NexoColor.parse(MSG_LEVEL_UP_TITLE));
            player.sendMessage(NexoColor.parse(MSG_LEVEL_UP_DESC.replace("%item%", item.displayName())));
            player.sendMessage(NexoColor.parse(BC_DIVIDER));

            // Si alcanzó el máximo nivel, anunciarlo a todo el servidor
            if (nivelNuevo == TIERS.length) {
                Bukkit.broadcast(NexoColor.parse(" "));
                Bukkit.broadcast(NexoColor.parse(BC_MAX_LEVEL.replace("%player%", player.getName()).replace("%max%", String.valueOf(TIERS.length)).replace("%item%", item.displayName())));
                Bukkit.broadcast(NexoColor.parse(" "));
            }
        }
    }

    // 🏆 SISTEMA DE TOP GLOBAL (SUPABASE)
    public void calcularTopAsync(Player player, String itemId) {
        itemId = itemId.toUpperCase();
        if (!itemsRegistrados.containsKey(itemId)) {
            player.sendMessage(NexoColor.parse(ERR_COLLECTION_404));
            return;
        }

        CollectionItem cItem = itemsRegistrados.get(itemId);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // Consulta SQL que extrae el valor de la key del JSONB
            String sql = "SELECT j.name, CAST(c.collections_data->>? AS INTEGER) as amount " +
                    "FROM nexo_collections c " +
                    "JOIN jugadores j ON c.uuid = j.uuid " +
                    "WHERE c.collections_data ? ? " +
                    "ORDER BY amount DESC LIMIT 5";

            try (Connection conn = NexoCore.getPlugin(NexoCore.class).getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, cItem.itemId());
                ps.setString(2, cItem.itemId());
                ResultSet rs = ps.executeQuery();

                // 🌟 Guardamos directamente el Component de Paper
                List<net.kyori.adventure.text.Component> lineasTop = new ArrayList<>();
                int rank = 1;
                while (rs.next()) {
                    String pName = rs.getString("name");
                    int amt = rs.getInt("amount");
                    lineasTop.add(NexoColor.parse(MSG_TOP_FORMAT.replace("%rank%", String.valueOf(rank)).replace("%player%", pName).replace("%amount%", String.valueOf(amt))));
                    rank++;
                }

                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(NexoColor.parse(BC_DIVIDER));
                    player.sendMessage(NexoColor.parse(MSG_TOP_TITLE.replace("%item%", cItem.displayName())));

                    if (lineasTop.isEmpty()) {
                        player.sendMessage(NexoColor.parse(MSG_TOP_EMPTY));
                    } else {
                        // 🌟 Imprimimos el componente en pantalla
                        for (net.kyori.adventure.text.Component l : lineasTop) {
                            player.sendMessage(l);
                        }
                    }
                    player.sendMessage(NexoColor.parse(BC_DIVIDER));
                });

            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(NexoColor.parse(ERR_TOP_LOAD)));
            }
        });
    }
}