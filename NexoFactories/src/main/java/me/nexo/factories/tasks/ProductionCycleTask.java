package me.nexo.factories.tasks;

import me.nexo.factories.NexoFactories;
import me.nexo.factories.core.ActiveFactory;
import me.nexo.protections.NexoProtections;
import me.nexo.protections.core.ProtectionStone;
import org.bukkit.scheduler.BukkitRunnable;

public class ProductionCycleTask extends BukkitRunnable {

    private final NexoFactories plugin;
    private final double ENERGY_COST_PER_CYCLE = 15.0; // Costo de energía por minuto

    public ProductionCycleTask(NexoFactories plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        // Recorremos todas las máquinas que están en la RAM (O(1) súper rápido)
        for (ActiveFactory factory : plugin.getFactoryManager().getCache().asMap().values()) {

            // 1. Buscamos la piedra de protección vinculada a esta máquina
            // (Nota: Si tu método en ClaimManager se llama distinto, ajusta 'getStoneById' a 'getStone')
            ProtectionStone stone = NexoProtections.getClaimManager().getStoneById(factory.getStoneId());

            if (stone == null) {
                factory.setCurrentStatus("NO_STONE"); // La piedra fue destruida
                plugin.getFactoryManager().saveFactoryStatusAsync(factory);
                continue;
            }

            // 2. Verificamos si la piedra tiene energía suficiente
            if (stone.getCurrentEnergy() >= ENERGY_COST_PER_CYCLE) {

                // ⚡ DRENAMOS LA ENERGÍA
                stone.drainEnergy(ENERGY_COST_PER_CYCLE);

                // 📦 PRODUCIMOS LOS ÍTEMS (Fórmula base: Nivel * 2)
                int generatedItems = factory.getLevel() * 2;
                factory.addOutput(generatedItems);
                factory.setCurrentStatus("ACTIVE");

                // Guardamos el progreso asíncronamente en Supabase
                plugin.getFactoryManager().saveFactoryStatusAsync(factory);

            } else {
                // ❌ APAGÓN: No hay energía
                if (!factory.getCurrentStatus().equals("NO_ENERGY")) {
                    factory.setCurrentStatus("NO_ENERGY");
                    plugin.getFactoryManager().saveFactoryStatusAsync(factory);
                }
            }
        }
    }
}