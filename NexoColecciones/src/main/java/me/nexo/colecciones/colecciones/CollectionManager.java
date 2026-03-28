package me.nexo.colecciones.colecciones;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import me.nexo.colecciones.NexoColecciones;
import me.nexo.colecciones.data.CollectionCategory;
import me.nexo.colecciones.data.CollectionItem;
import me.nexo.colecciones.data.Tier;
import me.nexo.core.NexoCore;
import me.nexo.core.crossplay.CrossplayUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CollectionManager {

    private final NexoColecciones plugin;

    private Map<String, CollectionCategory> categoriasRegistradas = new HashMap<>();
    private final Map<UUID, CollectionProfile> perfilesJugadores = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();

    public CollectionManager(NexoColecciones plugin) {
        this.plugin = plugin;
    }

    public void cargarDesdeConfig() {
        this.categoriasRegistradas = plugin.getColeccionesConfig().cargarCategoriasEnRam();
        plugin.getLogger().info("Se han cargado " + categoriasRegistradas.size() + " categorías de colecciones.");
    }

    public void loadPlayerFromDatabase(UUID uuid, DataSource dataSource) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "SELECT collections_data, claimed_tiers FROM nexo_collections WHERE uuid = ?";
            try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    String jsonProgress = rs.getString("collections_data");
                    String jsonClaimed = rs.getString("claimed_tiers");
                    Map<String, Integer> mapProgress = gson.fromJson(jsonProgress, new TypeToken<Map<String, Integer>>() {}.getType());
                    Map<String, Set<Integer>> mapClaimed = gson.fromJson(jsonClaimed, new TypeToken<Map<String, Set<Integer>>>() {}.getType());
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

    public void addProgress(Player player, String itemId, int amount) {
        itemId = itemId.toLowerCase();
        CollectionItem item = getItemGlobal(itemId);
        if (item == null) return;

        CollectionProfile profile = perfilesJugadores.get(player.getUniqueId());
        if (profile == null) return;

        int nivelViejo = calcularNivel(item, profile.getProgress(itemId));
        profile.addProgress(itemId, amount);
        int nivelNuevo = calcularNivel(item, profile.getProgress(itemId));

        if (nivelNuevo > nivelViejo) {
            CrossplayUtils.sendTitle(player,
                    plugin.getConfigManager().getMessage("eventos.subida-nivel.titulo-pantalla").replace("%level%", String.valueOf(nivelNuevo)),
                    plugin.getConfigManager().getMessage("eventos.subida-nivel.subtitulo-pantalla").replace("%item_name%", item.getNombre()));

            CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.colecciones.top-divisor"));
            CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("eventos.subida-nivel.titulo"));
            CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("eventos.subida-nivel.descripcion")
                    .replace("%level%", String.valueOf(nivelNuevo)).replace("%item%", item.getNombre()));
            CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.colecciones.top-divisor"));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);

            if (nivelNuevo == item.getMaxTier()) {
                CrossplayUtils.broadcastMessage(" ");
                CrossplayUtils.broadcastMessage(plugin.getConfigManager().getMessage("eventos.max-nivel")
                        .replace("%player%", player.getName()).replace("%item%", item.getNombre()));
                CrossplayUtils.broadcastMessage(" ");
            }
        }
    }

    public int calcularNivel(CollectionItem item, int cantidadFarmeada) {
        int nivelAlcanzado = 0;
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

    public void reclamarRecompensa(Player player, String itemId, int targetTier) {
        CollectionProfile profile = perfilesJugadores.get(player.getUniqueId());
        if (profile == null) return;
        CollectionItem item = getItemGlobal(itemId);
        if (item == null) return;
        Tier tier = item.getTier(targetTier);
        if (tier == null) return;
        if (profile.getProgress(itemId) < tier.getRequerido()) return;
        if (profile.hasClaimedTier(itemId, targetTier)) return;

        ejecutarRecompensas(player, tier.getRecompensas());
        profile.markTierAsClaimed(itemId, targetTier);

        CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("eventos.recompensa-reclamada"));
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
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + pName + " permission set " + perm + " true");
            }
        }
    }

    public void calcularTopAsync(Player player, String itemId) {
        CollectionItem cItem = getItemGlobal(itemId);
        if (cItem == null) {
            CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.colecciones.top-error"));
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "SELECT j.name, CAST(c.collections_data->>? AS INTEGER) as amount " +
                    "FROM nexo_collections c " +
                    "JOIN jugadores j ON c.uuid = j.uuid " +
                    "WHERE c.collections_data ? ? " +
                    "ORDER BY amount DESC LIMIT 5";
            try (Connection conn = NexoCore.getPlugin(NexoCore.class).getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, cItem.getId());
                ps.setString(2, cItem.getId());
                ResultSet rs = ps.executeQuery();
                List<String> lineasTop = new ArrayList<>();
                int rank = 1;
                while (rs.next()) {
                    String pName = rs.getString("name");
                    int amt = rs.getInt("amount");
                    lineasTop.add(plugin.getConfigManager().getMessage("comandos.colecciones.top-linea")
                            .replace("%rank%", String.valueOf(rank))
                            .replace("%player%", pName)
                            .replace("%amount%", String.valueOf(amt)));
                    rank++;
                }
                Bukkit.getScheduler().runTask(plugin, () -> {
                    CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.colecciones.top-divisor"));
                    CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.colecciones.top-titulo").replace("%collection_name%", cItem.getNombre()));
                    if (lineasTop.isEmpty()) {
                        CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.colecciones.top-vacio"));
                    } else {
                        for (String l : lineasTop) {
                            CrossplayUtils.sendMessage(player, l);
                        }
                    }
                    CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.colecciones.top-divisor"));
                });
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () -> CrossplayUtils.sendMessage(player, "&#8b0000[!] Error crítico de red al contactar con la base de datos."));
            }
        });
    }

    public CollectionItem getItemGlobal(String itemId) {
        for (CollectionCategory cat : categoriasRegistradas.values()) {
            if (cat.getItems().containsKey(itemId.toLowerCase())) {
                return cat.getItems().get(itemId.toLowerCase());
            }
        }
        return null;
    }

    public Map<String, CollectionCategory> getCategorias() {
        return categoriasRegistradas;
    }

    public CollectionProfile getProfile(UUID uuid) {
        return perfilesJugadores.get(uuid);
    }

    public void removeProfile(UUID uuid) {
        perfilesJugadores.remove(uuid);
    }

    public Map<UUID, CollectionProfile> getPerfiles() {
        return perfilesJugadores;
    }
}