package me.nexo.factories.core;

import org.bukkit.Location;
import java.util.UUID;

public class ActiveFactory {

    private final UUID id;
    private final UUID stoneId;
    private final UUID ownerId;
    private final String factoryType;
    private int level;
    private String currentStatus;
    private int storedOutput;
    private final Location coreLocation;

    // 🌟 AUTOMATIZACIÓN (Logic Engine)
    private String catalystItem;
    private String jsonLogic;

    // ⏳ NEXO ARCHITECT V3: Control de Tiempo (Timestamp Diff)
    private long lastEvaluationTime;

    public ActiveFactory(UUID id, UUID stoneId, UUID ownerId, String factoryType, int level, String currentStatus, int storedOutput, Location coreLocation, String catalystItem, String jsonLogic, long lastEvaluationTime) {
        this.id = id;
        this.stoneId = stoneId;
        this.ownerId = ownerId;
        this.factoryType = factoryType;
        this.level = level;
        this.currentStatus = currentStatus;
        this.storedOutput = storedOutput;
        this.coreLocation = coreLocation;
        this.catalystItem = catalystItem;
        this.jsonLogic = jsonLogic;
        this.lastEvaluationTime = lastEvaluationTime; // 🌟 Inicializado
    }

    public synchronized void addOutput(int amount) { this.storedOutput += amount; }
    public synchronized void clearOutput() { this.storedOutput = 0; }

    public UUID getId() { return id; }
    public UUID getStoneId() { return stoneId; }
    public UUID getOwnerId() { return ownerId; }
    public String getFactoryType() { return factoryType; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public String getCurrentStatus() { return currentStatus; }
    public void setCurrentStatus(String currentStatus) { this.currentStatus = currentStatus; }
    public int getStoredOutput() { return storedOutput; }
    public Location getCoreLocation() { return coreLocation; }

    public String getCatalystItem() { return catalystItem; }
    public void setCatalystItem(String catalystItem) { this.catalystItem = catalystItem; }
    public String getJsonLogic() { return jsonLogic; }
    public void setJsonLogic(String jsonLogic) { this.jsonLogic = jsonLogic; }

    // ⏳ Getters y Setters de Tiempo
    public long getLastEvaluationTime() { return lastEvaluationTime; }
    public void setLastEvaluationTime(long time) { this.lastEvaluationTime = time; }
}