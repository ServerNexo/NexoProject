package me.nexo.colecciones.colecciones;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.zaxxer.hikari.HikariDataSource;
import me.nexo.colecciones.NexoColecciones;
import me.nexo.colecciones.data.CollectionCategory;
import me.nexo.colecciones.data.CollectionItem;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
        registrarColeccionesEjemplo();
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
    }

    public int calcularNivel(int cantidad) {
        for (int i = TIERS.length - 1; i >= 0; i--) {
            if (cantidad >= TIERS[i]) return i + 1;
        }
        return 0;
    }

    private void registrarColeccionesEjemplo() {
        Map<Integer, List<String>> robleRewards = new HashMap<>();
        robleRewards.put(1, List.of("give %player% wooden_axe 1"));
        robleRewards.put(5, List.of("nexoitems give %player% madera_refinada 1"));
        itemsRegistrados.put("OAK_LOG", new CollectionItem("OAK_LOG", CollectionCategory.TALA, "Madera de Roble", robleRewards));
    }

    // ==========================================
    // METODOS DE BASE DE DATOS Y RAM
    // ==========================================
    public CollectionProfile getProfile(UUID uuid) { return perfiles.get(uuid); }
    public void removeProfile(UUID uuid) { perfiles.remove(uuid); }
    public ConcurrentHashMap<UUID, CollectionProfile> getPerfiles() { return perfiles; }

    public void loadPlayerFromDatabase(UUID uuid, HikariDataSource hikari) {
        String sql = "SELECT collections_data FROM nexo_collections WHERE uuid = ?";

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = hikari.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();

                ConcurrentHashMap<String, Integer> progress = new ConcurrentHashMap<>();
                if (rs.next()) {
                    String json = rs.getString("collections_data");
                    Type type = new TypeToken<ConcurrentHashMap<String, Integer>>(){}.getType();
                    progress = gson.fromJson(json, type);
                }

                perfiles.put(uuid, new CollectionProfile(uuid, progress));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}