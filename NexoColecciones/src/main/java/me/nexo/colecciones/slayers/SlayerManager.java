package me.nexo.colecciones.slayers;

import me.nexo.colecciones.NexoColecciones;
import me.nexo.core.NexoCore;
import me.nexo.core.crossplay.CrossplayUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SlayerManager {

    private final NexoColecciones plugin;
    private final NexoCore core;

    public record SlayerTemplate(String id, String name, String targetMob, int requiredKills, String bossName, String bossType) {}

    private final Map<String, SlayerTemplate> templates = new HashMap<>();
    private final Map<UUID, ActiveSlayer> activeSlayers = new ConcurrentHashMap<>();

    public SlayerManager(NexoColecciones plugin) {
        this.plugin = plugin;
        this.core = NexoCore.getPlugin(NexoCore.class);
    }

    private String getMessage(String path) {
        return core.getConfigManager().getMessage("colecciones_messages.yml", path);
    }

    public void cargarSlayers() {
        templates.clear();
        File file = new File(plugin.getDataFolder(), "slayers.yml");
        if (!file.exists()) plugin.saveResource("slayers.yml", false);

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            String name = config.getString(key + ".nombre", key);
            String targetMob = config.getString(key + ".mob_objetivo", "ZOMBIE");
            int kills = config.getInt(key + ".kills_necesarias", 100);
            String bossName = config.getString(key + ".boss_nombre", "Boss");
            String bossType = config.getString(key + ".boss_tipo", "ZOMBIE");

            templates.put(key.toUpperCase(), new SlayerTemplate(key.toUpperCase(), name, targetMob, kills, bossName, bossType));
        }
    }

    public Map<String, SlayerTemplate> getTemplates() { return templates; }
    public ActiveSlayer getActiveSlayer(UUID uuid) { return activeSlayers.get(uuid); }
    public void removeActiveSlayer(UUID uuid) { activeSlayers.remove(uuid); }

    public void iniciarSlayer(Player player, String slayerId) {
        slayerId = slayerId.toUpperCase();

        if (!templates.containsKey(slayerId)) {
            CrossplayUtils.sendMessage(player, getMessage("comandos.slayer.no-encontrado"));
            return;
        }

        if (activeSlayers.containsKey(player.getUniqueId())) {
            CrossplayUtils.sendMessage(player, getMessage("comandos.slayer.ya-activo"));
            return;
        }

        SlayerTemplate template = templates.get(slayerId);
        ActiveSlayer activo = new ActiveSlayer(player, template);
        activeSlayers.put(player.getUniqueId(), activo);

        CrossplayUtils.sendMessage(player, getMessage("comandos.slayer.inicio-caceria"));
        CrossplayUtils.sendMessage(player, getMessage("comandos.slayer.contrato-aceptado").replace("%name%", template.name()));
        CrossplayUtils.sendMessage(player, getMessage("comandos.slayer.descripcion-caceria").replace("%kills%", String.valueOf(template.requiredKills())).replace("%mob%", template.targetMob()));
    }
}