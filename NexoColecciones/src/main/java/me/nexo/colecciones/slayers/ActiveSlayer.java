package me.nexo.colecciones.slayers;

import org.bukkit.boss.BossBar;
import java.util.List;
import java.util.UUID;

public class ActiveSlayer {
    private final UUID playerId;
    private final String slayerId;
    private final String targetMob;
    private final int requiredKills;
    private int currentKills;
    private final String bossType;
    private final String bossName;
    private final List<String> rewards;
    private boolean bossSpawned;

    // 🌟 NUEVO: Variable para guardar la BossBar del Boss
    private BossBar bossBar;

    public ActiveSlayer(UUID playerId, String slayerId, String targetMob, int requiredKills, String bossType, String bossName, List<String> rewards) {
        this.playerId = playerId;
        this.slayerId = slayerId;
        this.targetMob = targetMob;
        this.requiredKills = requiredKills;
        this.currentKills = 0;
        this.bossType = bossType;
        this.bossName = bossName;
        this.rewards = rewards;
        this.bossSpawned = false;
        this.bossBar = null;
    }

    public void addKill() {
        if (!bossSpawned && currentKills < requiredKills) {
            this.currentKills++;
        }
    }

    public boolean isReadyForBoss() {
        return currentKills >= requiredKills;
    }

    // Getters y Setters
    public UUID getPlayerId() { return playerId; }
    public String getSlayerId() { return slayerId; }
    public String getTargetMob() { return targetMob; }
    public int getRequiredKills() { return requiredKills; }
    public int getCurrentKills() { return currentKills; }
    public String getBossType() { return bossType; }
    public String getBossName() { return bossName; }
    public List<String> getRewards() { return rewards; }
    public boolean isBossSpawned() { return bossSpawned; }
    public void setBossSpawned(boolean bossSpawned) { this.bossSpawned = bossSpawned; }
    public BossBar getBossBar() { return bossBar; }
    public void setBossBar(BossBar bossBar) { this.bossBar = bossBar; }
}