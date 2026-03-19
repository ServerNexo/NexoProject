package me.nexo.minions.manager;

import me.nexo.colecciones.colecciones.CollectionManager;
import me.nexo.colecciones.colecciones.CollectionProfile;
import me.nexo.core.user.NexoAPI;
import me.nexo.minions.NexoMinions;
import me.nexo.minions.data.MinionKeys;
import me.nexo.minions.data.MinionTier;
import me.nexo.minions.data.MinionType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.UUID;

public class ActiveMinion {
    private final NexoMinions plugin;
    private final ItemDisplay entity;
    private final Interaction hitbox;
    private final UUID ownerId;
    private final MinionType type;
    private int tier;
    private long nextActionTime;
    private int storedItems;
    private final ItemStack[] upgrades = new ItemStack[4];
    private int trabajosRealizados = 0;

    public ActiveMinion(NexoMinions plugin, ItemDisplay entity, Interaction hitbox, UUID ownerId, MinionType type, int tier, long nextActionTime, int storedItems) {
        this.plugin = plugin;
        this.entity = entity;
        this.hitbox = hitbox;
        this.ownerId = ownerId;
        this.type = type;
        this.tier = tier;
        this.nextActionTime = nextActionTime;
        this.storedItems = storedItems;

        for (int i = 0; i < 4; i++) {
            byte[] bytes = entity.getPersistentDataContainer().get(MinionKeys.UPGRADES[i], PersistentDataType.BYTE_ARRAY);
            if (bytes != null) this.upgrades[i] = ItemStack.deserializeBytes(bytes);
        }
    }

    public void tick(long currentTimeMillis) {
        if (storedItems >= MinionTier.getMaxStorage(tier) && !tieneMejora("STORAGE_LINK")) {
            animar(); return;
        }

        if (currentTimeMillis >= nextActionTime) {
            realizarTrabajo();
            long tiempoBase = MinionTier.getDelayMillis(tier);
            nextActionTime = currentTimeMillis + (long) (tiempoBase * getSpeedMultiplier());
            entity.getPersistentDataContainer().set(MinionKeys.NEXT_ACTION, PersistentDataType.LONG, nextActionTime);
        }
        animar();
    }

    private void realizarTrabajo() {
        Location loc = entity.getLocation();
        loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc.clone().add(0, 0.5, 0), 3);
        loc.getWorld().playSound(loc, Sound.ENTITY_BEE_POLLINATE, 0.2f, 1.5f);

        boolean guardadoEnCofre = false;
        if (tieneMejora("STORAGE_LINK")) {
            guardadoEnCofre = guardarEnCofreAdyacente(new ItemStack(type.getTargetMaterial(), 1));
        }

        if (!guardadoEnCofre) {
            this.storedItems += 1;
            entity.getPersistentDataContainer().set(MinionKeys.STORED_ITEMS, PersistentDataType.INTEGER, this.storedItems);
        }

        this.trabajosRealizados++;
        consumirCombustibles();

        Player owner = Bukkit.getPlayer(ownerId);
        if (owner != null && owner.isOnline()) {

            // 1. Colecciones
            String blockId = type.getTargetMaterial().name().toLowerCase();
            // 1. Obtener el perfil desde el nuevo Manager de Colecciones
            me.nexo.colecciones.colecciones.CollectionProfile profile = me.nexo.colecciones.colecciones.CollectionManager.getProfile(ownerId);

            if (profile != null) {
                profile.addProgress(blockId, 1, false);
            }

            // 🟢 2. ARQUITECTURA LIMPIA: NexoMinions pide a NexoCore que entregue la XP
            String tipoMinion = type.name();

            if (tipoMinion.contains("WHEAT") || tipoMinion.contains("CARROT") || tipoMinion.contains("POTATO") || tipoMinion.contains("MELON") || tipoMinion.contains("PUMPKIN") || tipoMinion.contains("SUGAR_CANE")) {
                NexoAPI.getInstance().addAgriculturaXpAsync(ownerId, 2);
            }
            else if (tipoMinion.contains("ORE") || tipoMinion.contains("COBBLESTONE") || tipoMinion.contains("STONE") || tipoMinion.contains("OBSIDIAN")) {
                NexoAPI.getInstance().addMineriaXpAsync(ownerId, 2);
            }
        }
    }

    public double getSpeedMultiplier() {
        double multiplicador = 1.0;
        for (ItemStack item : upgrades) {
            ConfigurationSection datos = plugin.getUpgradesConfig().getUpgradeData(item);
            if (datos == null) continue;
            String tipo = datos.getString("tipo", "");
            if (tipo.equals("SPEED_TEMP") || tipo.equals("SPEED_PERM")) {
                multiplicador -= datos.getDouble("bonus_velocidad", 0.0);
            }
        }
        return Math.max(multiplicador, 0.2);
    }

    private void consumirCombustibles() {
        for (int i = 0; i < 4; i++) {
            ItemStack item = upgrades[i];
            ConfigurationSection datos = plugin.getUpgradesConfig().getUpgradeData(item);
            if (datos != null && datos.getString("tipo", "").equals("SPEED_TEMP")) {
                int usosReq = datos.getInt("usos", 20);
                if (trabajosRealizados >= usosReq) {
                    item.setAmount(item.getAmount() - 1);
                    setUpgrade(i, item);
                    trabajosRealizados = 0;
                    break;
                }
            }
        }
    }

    public boolean tieneMejora(String tipoBuscado) {
        for (ItemStack item : upgrades) {
            ConfigurationSection datos = plugin.getUpgradesConfig().getUpgradeData(item);
            if (datos != null && datos.getString("tipo", "").equals(tipoBuscado)) return true;
        }
        return false;
    }

    private boolean guardarEnCofreAdyacente(ItemStack item) {
        int[][] offsets = {{1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}};
        for (int[] offset : offsets) {
            Block b = entity.getLocation().clone().add(offset[0], 0, offset[2]).getBlock();
            if (b.getState() instanceof InventoryHolder holder) {
                var sobrante = holder.getInventory().addItem(item);
                if (sobrante.isEmpty()) return true;
            }
        }
        return false;
    }

    public ItemStack[] getUpgrades() { return upgrades; }
    public void setUpgrade(int slot, ItemStack item) {
        upgrades[slot] = item;
        if (item == null || item.getType().isAir()) {
            entity.getPersistentDataContainer().remove(MinionKeys.UPGRADES[slot]);
        } else {
            entity.getPersistentDataContainer().set(MinionKeys.UPGRADES[slot], PersistentDataType.BYTE_ARRAY, item.serializeAsBytes());
        }
    }

    public void setTier(int nuevoTier) {
        this.tier = nuevoTier;
        entity.getPersistentDataContainer().set(MinionKeys.TIER, PersistentDataType.INTEGER, nuevoTier);
    }

    private void animar() {
        Transformation trans = entity.getTransformation();
        float nuevoAngulo = (System.currentTimeMillis() % 4000) / 4000f * (float) Math.PI * 2;
        trans.getLeftRotation().set(new AxisAngle4f(nuevoAngulo, new Vector3f(0, 1, 0)));
        entity.setTransformation(trans);
    }

    public ItemDisplay getEntity() { return entity; }
    public Interaction getHitbox() { return hitbox; }
    public MinionType getType() { return type; }
    public int getTier() { return tier; }
    public int getStoredItems() { return storedItems; }
    public void setStoredItems(int storedItems) { this.storedItems = storedItems; }
}