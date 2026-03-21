package me.nexo.clans.core;

import java.math.BigDecimal;
import java.util.UUID;

public class NexoClan {

    private final UUID id;
    private String name;
    private String tag;

    // Progresión
    private int monolithLevel;
    private long monolithExp;

    // Economía
    private BigDecimal bankBalance;

    // Turismo (Guardaremos la ubicación como un String serializado por ahora)
    private String publicHome;

    public NexoClan(UUID id, String name, String tag, int monolithLevel, long monolithExp, BigDecimal bankBalance, String publicHome) {
        this.id = id;
        this.name = name;
        this.tag = tag;
        this.monolithLevel = monolithLevel;
        this.monolithExp = monolithExp;
        this.bankBalance = bankBalance;
        this.publicHome = publicHome;
    }

    // ==========================================
    // 🛡️ MÉTODOS SINCRONIZADOS (Thread-Safe)
    // ==========================================
    public synchronized void addMonolithExp(long amount) {
        this.monolithExp += amount;
        // Aquí luego pondremos la lógica de subir de nivel
    }

    public synchronized void depositMoney(double amount) {
        if (amount > 0) {
            this.bankBalance = this.bankBalance.add(BigDecimal.valueOf(amount));
        }
    }

    public synchronized void withdrawMoney(double amount) {
        if (amount > 0 && hasEnoughMoney(amount)) {
            this.bankBalance = this.bankBalance.subtract(BigDecimal.valueOf(amount));
        }
    }

    public boolean hasEnoughMoney(double amount) {
        return this.bankBalance.compareTo(BigDecimal.valueOf(amount)) >= 0;
    }

    // ==========================================
    // 📖 GETTERS Y SETTERS BÁSICOS
    // ==========================================
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
}