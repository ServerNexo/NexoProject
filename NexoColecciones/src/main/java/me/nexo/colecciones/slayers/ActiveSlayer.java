package me.nexo.colecciones.slayers;

import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ActiveSlayer {

    private final UUID playerId;
    private final SlayerManager.SlayerTemplate template;

    private int currentKills;
    private boolean bossSpawned;
    private BossBar bossBar;

    public ActiveSlayer(Player player, SlayerManager.SlayerTemplate template) {
        this.playerId = player.getUniqueId();
        this.template = template;
        this.currentKills = 0;
        this.bossSpawned = false;
        this.bossBar = null;
    }

    public void addKill() {
        if (!bossSpawned && currentKills < template.requiredKills()) {
            this.currentKills++;
        }
    }

    // 🌟 GETTERS Y SETTERS COMPATIBLES
    public UUID getPlayerId() { return playerId; }

    public SlayerManager.SlayerTemplate getTemplate() { return template; }

    public int getKills() { return currentKills; }

    public String getBossName() { return template.bossName(); }

    public boolean isBossSpawned() { return bossSpawned; }
    public void setBossSpawned(boolean bossSpawned) { this.bossSpawned = bossSpawned; }

    public BossBar getBossBar() { return bossBar; }
    public void setBossBar(BossBar bossBar) { this.bossBar = bossBar; }
}