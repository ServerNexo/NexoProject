package me.nexo.colecciones.slayers;

import me.nexo.colecciones.NexoColecciones;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SlayerManager {

    private final NexoColecciones plugin;

    // Aquí guardamos los Slayers Activos de los jugadores online
    private final Map<UUID, ActiveSlayer> activeSlayers = new HashMap<>();

    // Aquí guardamos las plantillas cargadas desde colecciones.yml
    private final Map<String, SlayerTemplate> templates = new HashMap<>();

    public SlayerManager(NexoColecciones plugin) {
        this.plugin = plugin;
        cargarSlayers();
    }

    public void cargarSlayers() {
        templates.clear();
        ConfigurationSection sec = plugin.getColeccionesConfig().getConfig().getConfigurationSection("slayers");
        if (sec == null) {
            plugin.getLogger().warning("No se encontró la sección 'slayers' en colecciones.yml.");
            return;
        }

        for (String key : sec.getKeys(false)) {
            ConfigurationSection data = sec.getConfigurationSection(key);
            if (data == null) continue;

            templates.put(key.toUpperCase(), new SlayerTemplate(
                    key.toUpperCase(),
                    data.getString("nombre", key).replace("&", "§"),
                    data.getString("mob_para_farmear", "ZOMBIE").toUpperCase(),
                    data.getInt("kills_requeridas", 50),
                    data.getString("boss_tipo", "ZOMBIE").toUpperCase(),
                    data.getString("boss_nombre", "Boss").replace("&", "§"),
                    data.getStringList("recompensas")
            ));
        }
        plugin.getLogger().info("⚔️ Se han cargado " + templates.size() + " misiones de Slayer desde la configuración.");
    }

    public void iniciarSlayer(Player player, String slayerId) {
        SlayerTemplate template = templates.get(slayerId.toUpperCase());
        if (template == null) {
            player.sendMessage("§cEsa misión de Slayer no existe.");
            return;
        }

        if (activeSlayers.containsKey(player.getUniqueId())) {
            player.sendMessage("§c¡Ya tienes un Slayer activo! Termínalo o cancélalo primero.");
            return;
        }

        ActiveSlayer nuevoSlayer = new ActiveSlayer(
                player.getUniqueId(), template.id(), template.targetMob(),
                template.requiredKills(), template.bossType(), template.bossName(), template.rewards()
        );

        activeSlayers.put(player.getUniqueId(), nuevoSlayer);

        player.sendMessage("§8=======================================");
        player.sendMessage("§c§l⚔️ ¡CACERÍA INICIADA! ⚔️");
        player.sendMessage("§7Has comenzado: §e" + template.name());
        player.sendMessage("§7Mata §c" + template.requiredKills() + " " + template.targetMob() + "s §7para invocar al Boss.");
        player.sendMessage("§8=======================================");
    }

    public ActiveSlayer getActiveSlayer(UUID uuid) {
        return activeSlayers.get(uuid);
    }

    public void removeActiveSlayer(UUID uuid) {
        activeSlayers.remove(uuid);
    }

    public Map<String, SlayerTemplate> getTemplates() {
        return templates;
    }

    // 🌟 Record súper optimizado para guardar las plantillas
    public record SlayerTemplate(String id, String name, String targetMob, int requiredKills, String bossType, String bossName, List<String> rewards) {}
}