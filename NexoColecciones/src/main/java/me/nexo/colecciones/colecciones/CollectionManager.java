package me.nexo.colecciones.colecciones;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.zaxxer.hikari.HikariDataSource;
import me.nexo.colecciones.NexoColecciones;
import me.nexo.colecciones.data.CollectionCategory;
import me.nexo.colecciones.data.CollectionItem;
import me.nexo.core.NexoCore;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CollectionManager {

    private final NexoColecciones plugin;
    private final ConcurrentHashMap<UUID, CollectionProfile> perfiles = new ConcurrentHashMap<>();
    private final Map<String, CollectionItem> itemsRegistrados = new HashMap<>();
    private final Gson gson = new Gson();

    public static final int[] TIERS = {
            50, 100, 250, 500, 1000, 2500, 5000, 10000, 25000, 50000, 100000, 250000, 500000, 1000000, 2500000
    };

    public CollectionManager(NexoColecciones plugin) {
        this.plugin = plugin;
        cargarDesdeConfig();
    }

    public void addProgress(Player player, String itemId, int amount) {
        CollectionItem cItem = itemsRegistrados.get(itemId);
        if (cItem == null) return;

        CollectionProfile profile = perfiles.get(player.getUniqueId());
        if (profile == null) return;

        int cantidadAnterior = profile.getProgress(itemId);
        int nuevaCantidad = cantidadAnterior + amount;

        profile.addProgress(itemId, amount, false);

        int nivelAnterior = calcularNivel(cantidadAnterior);
        int nuevoNivel = calcularNivel(nuevaCantidad);

        // 📊 MECÁNICA 3: ACTION BAR EN TIEMPO REAL
        int siguienteMeta = (nuevoNivel < TIERS.length) ? TIERS[nuevoNivel] : nuevaCantidad;
        String actionMsg = "§e⭐ " + cItem.displayName() + "§e: §b" + nuevaCantidad;
        if (nuevoNivel < TIERS.length) {
            actionMsg += " §8/ §b" + siguienteMeta + " §7(Nivel " + nuevoNivel + ") §e⭐";
        } else {
            actionMsg += " §7(§6MAX§7) §e⭐";
        }
        player.sendActionBar(actionMsg);

        if (nuevoNivel > nivelAnterior) {
            dispararLevelUp(player, cItem, nuevoNivel);
        }
    }

    private void dispararLevelUp(Player player, CollectionItem item, int nuevoNivel) {
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        player.sendTitle("§6§lCOLECCIÓN", "§e" + item.displayName() + " §7Subió al Nivel §b" + nuevoNivel, 10, 60, 10);
        player.sendMessage("§8=======================================");
        player.sendMessage("§6§l⭐ ¡NUEVO NIVEL DE COLECCIÓN ALCANZADO!");
        player.sendMessage("§7Has desbloqueado nuevas recompensas en: §e" + item.displayName());
        player.sendMessage("§8=======================================");

        if (item.comandosRecompensa().containsKey(nuevoNivel)) {
            for (String cmd : item.comandosRecompensa().get(nuevoNivel)) {
                String comandoFinal = cmd.replace("%player%", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), comandoFinal);
            }
        }

        // 🎆 MECÁNICA 4: ANUNCIO GLOBAL Y FUEGOS ARTIFICIALES (Nivel Máximo)
        if (nuevoNivel == TIERS.length) {
            Bukkit.broadcastMessage("");
            Bukkit.broadcastMessage("§8[§6§lNEXO§8] §e¡El jugador §a" + player.getName() + " §eha alcanzado el §6§lNIVEL MÁXIMO (" + TIERS.length + ") §een la colección de §b" + item.displayName() + "§e!");
            Bukkit.broadcastMessage("");

            org.bukkit.entity.Firework fw = player.getWorld().spawn(player.getLocation(), org.bukkit.entity.Firework.class);
            org.bukkit.inventory.meta.FireworkMeta fm = fw.getFireworkMeta();
            fm.addEffect(org.bukkit.FireworkEffect.builder().flicker(true).trail(true).with(org.bukkit.FireworkEffect.Type.BALL_LARGE).withColor(org.bukkit.Color.YELLOW).withFade(org.bukkit.Color.ORANGE).build());
            fm.setPower(1);
            fw.setFireworkMeta(fm);
        }
    }

    // 🏆 MECÁNICA 1: MOTOR DE LEADERBOARDS (Consulta Súper Rápida a Supabase)
    public void calcularTopAsync(Player player, String itemId) {
        CollectionItem cItem = itemsRegistrados.get(itemId.toUpperCase());
        if (cItem == null) {
            player.sendMessage("§cEsa colección no existe.");
            return;
        }

        var hikari = NexoCore.getPlugin(NexoCore.class).getDatabaseManager().getDataSource();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // Usamos un JOIN para sacar el nombre del jugador directamente de la tabla principal
            String sql = "SELECT j.nombre, (c.collections_data->>?)::int AS amount " +
                    "FROM nexo_collections c " +
                    "LEFT JOIN jugadores j ON c.uuid = j.uuid " +
                    "WHERE c.collections_data ? ? " +
                    "ORDER BY amount DESC LIMIT 5";

            try (Connection conn = hikari.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, itemId.toUpperCase());
                ps.setString(2, itemId.toUpperCase());
                ResultSet rs = ps.executeQuery();

                List<String> lineas = new ArrayList<>();
                int pos = 1;
                while (rs.next()) {
                    String nombre = rs.getString("nombre");
                    if (nombre == null) nombre = "Desconocido";
                    int amount = rs.getInt("amount");
                    lineas.add("§6" + pos + ". §e" + nombre + " §8- §b" + amount + " §7farmeados");
                    pos++;
                }

                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage("§8=======================================");
                    player.sendMessage("§6§l🏆 TOP 5 GLOBAL: §e" + cItem.displayName());
                    if (lineas.isEmpty()) player.sendMessage("§7Nadie ha farmeado esto aún...");
                    else for (String l : lineas) player.sendMessage(l);
                    player.sendMessage("§8=======================================");
                });
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage("§cError al cargar el Top."));
            }
        });
    }

    public int calcularNivel(int cantidad) {
        for (int i = TIERS.length - 1; i >= 0; i--) {
            if (cantidad >= TIERS[i]) return i + 1;
        }
        return 0;
    }

    public void cargarDesdeConfig() {
        itemsRegistrados.clear();
        org.bukkit.configuration.file.FileConfiguration config = plugin.getColeccionesConfig().getConfig();
        if (config == null || !config.contains("colecciones")) return;

        org.bukkit.configuration.ConfigurationSection seccionPrincipal = config.getConfigurationSection("colecciones");
        if (seccionPrincipal == null) return;

        for (String categoriaStr : seccionPrincipal.getKeys(false)) {
            CollectionCategory categoria;
            try { categoria = CollectionCategory.valueOf(categoriaStr.toUpperCase()); }
            catch (IllegalArgumentException e) { continue; }

            org.bukkit.configuration.ConfigurationSection seccionCategoria = seccionPrincipal.getConfigurationSection(categoriaStr);
            if (seccionCategoria == null) continue;

            for (String itemId : seccionCategoria.getKeys(false)) {
                org.bukkit.configuration.ConfigurationSection itemData = seccionCategoria.getConfigurationSection(itemId);
                if (itemData == null) continue;

                String nombreBonito = itemData.getString("nombre", itemId).replace("&", "§");
                Map<Integer, List<String>> comandos = new HashMap<>();
                if (itemData.contains("recompensas")) {
                    org.bukkit.configuration.ConfigurationSection recompensas = itemData.getConfigurationSection("recompensas");
                    if (recompensas != null) {
                        for (String nivelStr : recompensas.getKeys(false)) {
                            try { comandos.put(Integer.parseInt(nivelStr), recompensas.getStringList(nivelStr)); }
                            catch (NumberFormatException ignored) {}
                        }
                    }
                }
                itemsRegistrados.put(itemId.toUpperCase(), new CollectionItem(itemId.toUpperCase(), categoria, nombreBonito, comandos));
            }
        }
    }

    public CollectionProfile getProfile(UUID uuid) { return perfiles.get(uuid); }
    public void removeProfile(UUID uuid) { perfiles.remove(uuid); }
    public ConcurrentHashMap<UUID, CollectionProfile> getPerfiles() { return perfiles; }
    public Map<String, CollectionItem> getItemsRegistrados() { return itemsRegistrados; }

    public void loadPlayerFromDatabase(UUID uuid, HikariDataSource hikari) {
        String sql = "SELECT collections_data FROM nexo_collections WHERE uuid = ?";
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = hikari.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                ConcurrentHashMap<String, Integer> progress = new ConcurrentHashMap<>();
                if (rs.next()) {
                    Type type = new TypeToken<ConcurrentHashMap<String, Integer>>(){}.getType();
                    progress = gson.fromJson(rs.getString("collections_data"), type);
                }
                perfiles.put(uuid, new CollectionProfile(uuid, progress));
            } catch (Exception e) { e.printStackTrace(); }
        });
    }
}