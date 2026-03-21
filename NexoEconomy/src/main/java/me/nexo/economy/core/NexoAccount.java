package me.nexo.economy.core;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class NexoAccount {

    public enum AccountType {
        PLAYER, CLAN, SERVER_ESCROW // Escrow es para el Bazar (Retención de fondos)
    }

    public enum Currency {
        COINS, GEMS, MANA
    }

    private final UUID ownerId;
    private final AccountType type;

    // Usamos BigDecimal para evitar el famoso error de decimales infinitos en Java (Ej: 0.1 + 0.2 = 0.30000000000000004)
    private BigDecimal coins;
    private BigDecimal gems;
    private BigDecimal mana;

    public boolean hasEnough(Currency currency, BigDecimal amount) {
        return switch (currency) {
            case COINS -> this.coins.compareTo(amount) >= 0;
            case GEMS -> this.gems.compareTo(amount) >= 0;
            case MANA -> this.mana.compareTo(amount) >= 0;
        };
    }

    public void addBalance(Currency currency, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return;
        switch (currency) {
            case COINS -> this.coins = this.coins.add(amount);
            case GEMS -> this.gems = this.gems.add(amount);
            case MANA -> this.mana = this.mana.add(amount);
        }
    }

    public void removeBalance(Currency currency, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return;
        switch (currency) {
            case COINS -> this.coins = this.coins.subtract(amount);
            case GEMS -> this.gems = this.gems.subtract(amount);
            case MANA -> this.mana = this.mana.subtract(amount);
        }
    }
}