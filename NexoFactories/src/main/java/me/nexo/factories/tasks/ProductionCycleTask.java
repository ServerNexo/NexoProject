package me.nexo.factories.tasks;

import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.skill.Skills;
import dev.aurelium.auraskills.api.user.SkillsUser;
import me.nexo.factories.NexoFactories;
import me.nexo.factories.core.ActiveFactory;
import me.nexo.factories.logic.ScriptEvaluator;
import me.nexo.protections.NexoProtections;
import me.nexo.protections.core.ProtectionStone;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class ProductionCycleTask extends BukkitRunnable {

    private final NexoFactories plugin;
    private final ScriptEvaluator logicEngine;
    private final double ENERGY_COST_PER_CYCLE = 15.0;

    public ProductionCycleTask(NexoFactories plugin) {
        this.plugin = plugin;
        this.logicEngine = new ScriptEvaluator();
    }

    @Override
    public void run() {
        for (ActiveFactory factory : plugin.getFactoryManager().getCache().asMap().values()) {

            ProtectionStone stone = NexoProtections.getClaimManager().getStoneById(factory.getStoneId());
            if (stone == null) {
                updateStatus(factory, "NO_STONE");
                continue;
            }

            // 1. EL MOTOR LÓGICO
            if (!logicEngine.shouldRun(factory, stone, factory.getJsonLogic())) {
                updateStatus(factory, "SCRIPT_PAUSED");
                continue;
            }

            // 2. CONSUMO DE ENERGÍA
            if (stone.getCurrentEnergy() >= ENERGY_COST_PER_CYCLE) {
                stone.drainEnergy(ENERGY_COST_PER_CYCLE);

                // 3. PRODUCCIÓN CON SINERGIA
                int baseProduction = factory.getLevel() * 2;
                double multiplier = getProfessionMultiplier(factory.getOwnerId(), factory.getFactoryType());

                // Si tiene el Catalizador Overclock, +50% de producción
                if (factory.getCatalystItem() != null && factory.getCatalystItem().equals("OVERCLOCK_T1")) {
                    multiplier += 0.5;
                }

                int finalOutput = (int) Math.round(baseProduction * multiplier);
                factory.addOutput(finalOutput);
                updateStatus(factory, "ACTIVE");

            } else {
                updateStatus(factory, "NO_ENERGY");
            }
        }
    }

    private void updateStatus(ActiveFactory factory, String status) {
        if (!factory.getCurrentStatus().equals(status)) {
            factory.setCurrentStatus(status);
            plugin.getFactoryManager().saveFactoryStatusAsync(factory);
        }
    }

    // 🌟 MÓDULO 4: INTEGRACIÓN REAL CON AURASKILLS
    private double getProfessionMultiplier(UUID ownerId, String factoryType) {
        try {
            SkillsUser user = AuraSkillsApi.get().getUser(ownerId);
            if (user != null) {
                int level = 1;

                // Determinamos la habilidad basándonos en el nombre de la máquina
                if (factoryType.contains("MINA") || factoryType.contains("FORJA")) {
                    level = user.getSkillLevel(Skills.MINING);
                } else if (factoryType.contains("ASERRADERO")) {
                    level = user.getSkillLevel(Skills.FORAGING);
                } else if (factoryType.contains("GRANJA")) {
                    level = user.getSkillLevel(Skills.FARMING);
                }

                return 1.0 + (level * 0.02); // +2% por cada nivel
            }
        } catch (NoClassDefFoundError | IllegalStateException e) {
            // Fallback silencioso por si AuraSkills no ha cargado aún
        }
        return 1.0;
    }
}