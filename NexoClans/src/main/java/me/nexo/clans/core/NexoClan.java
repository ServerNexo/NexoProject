package me.nexo.clans.core;

import java.math.BigDecimal;
import java.util.UUID;

public class NexoClan {

    private final UUID id;
    private String name;
    private String tag;

    private int monolithLevel;
    private long monolithExp;
    private BigDecimal bankBalance;
    private String publicHome;

    // 🌟 NUEVO: Fuego Amigo
    private boolean friendlyFire;

    public NexoClan(UUID id, String name, String tag, int monolithLevel, long monolithExp, BigDecimal bankBalance, String publicHome, boolean friendlyFire) {
        this.id = id;
        this.name = name;
        this.tag = tag;
        this.monolithLevel = monolithLevel;
        this.monolithExp = monolithExp;
        this.bankBalance = bankBalance;
        this.publicHome = publicHome;
        this.friendlyFire = friendlyFire; // 🌟 Inicializamos
    }

    // 🌟 CORREGIDO: Ahora devuelve boolean si subió de nivel y calcula la subida
    public synchronized boolean addMonolithExp(long amount) {
        this.monolithExp += amount;
        boolean levelUp = false;

        // Fórmula de ejemplo: Cada nivel requiere 1000 * nivel actual
        // Ej: Nivel 1 -> 1000 exp, Nivel 2 -> 2000 exp
        long expRequired = this.monolithLevel * 1000L;

        while (this.monolithExp >= expRequired) {
            this.monolithExp -= expRequired;
            this.monolithLevel++;
            levelUp = true;
            expRequired = this.monolithLevel * 1000L; // Recalculamos para el siguiente nivel
        }

        return levelUp;
    }

    // Métodos Sincronizados de Economía
    public synchronized void depositMoney(double amount) {
        if (amount > 0) this.bankBalance = this.bankBalance.add(BigDecimal.valueOf(amount));
    }
    public synchronized void withdrawMoney(double amount) {
        if (amount > 0 && hasEnoughMoney(amount)) this.bankBalance = this.bankBalance.subtract(BigDecimal.valueOf(amount));
    }
    public boolean hasEnoughMoney(double amount) { return this.bankBalance.compareTo(BigDecimal.valueOf(amount)) >= 0; }

    // Getters y Setters
    public UUID getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
    public int getMonolithLevel() { return monolithLevel; }
    public void setMonolithLevel(int monolithLevel) { this.monolithLevel = monolithLevel; }
    public long getMonolithExp() { return monolithExp; }
    public BigDecimal getBankBalance() { return bankBalance; }
    public String getPublicHome() { return publicHome; }
    public void setPublicHome(String publicHome) { this.publicHome = publicHome; }

    // 🌟 GETTER Y SETTER DE FUEGO AMIGO
    public boolean isFriendlyFire() { return friendlyFire; }
    public void setFriendlyFire(boolean friendlyFire) { this.friendlyFire = friendlyFire; }
}