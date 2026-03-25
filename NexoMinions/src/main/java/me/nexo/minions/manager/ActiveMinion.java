package me.nexo.minions.manager;

import me.nexo.colecciones.NexoColecciones;
import me.nexo.core.utils.NexoColor;
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
import org.bukkit.entity.TextDisplay;
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
    private final TextDisplay holograma;
    private final UUID ownerId;
    private final MinionType type;
    private int tier;
    private long nextActionTime;
    private int storedItems;
    private final ItemStack[] upgrades = new ItemStack[4];
    private int trabajosRealizados = 0;

    private InventoryHolder cachedStorage = null;
    private long lastStorageCheckTime = 0;

    public ActiveMinion(NexoMinions plugin, ItemDisplay entity, Interaction hitbox, TextDisplay holograma, UUID ownerId, MinionType type, int tier, long nextActionTime, int storedItems) {
        this.plugin = plugin;
        this.entity = entity;
        this.hitbox = hitbox;
        this.holograma = holograma;
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

    public int getRealMaxStorage() {
        int base = MinionTier.getMaxStorage(tier);
        int bonus = 0;

        for (ItemStack item : upgrades) {
            ConfigurationSection datos = plugin.getUpgradesConfig().getUpgradeData(item);
            if (datos != null && datos.getString("tipo", "").equals("STORAGE")) {
                bonus += datos.getInt("bonus_capacidad", 0);
            }
        }
        return base + bonus;
    }

    public void calcularTrabajoOffline(long currentTimeMillis) {
        long tiempoPasado = currentTimeMillis - this.nextActionTime;
        if (tiempoPasado > 0) {
            long tiempoPorAccion = (long) (MinionTier.getDelayMillis(this.tier) * getSpeedMultiplier());
            int trabajosPerdidos = (int) (tiempoPasado / tiempoPorAccion);

            int maxStorage = getRealMaxStorage();

            if (!tieneMejora("STORAGE_LINK")) {
                this.storedItems = Math.min(maxStorage, this.storedItems + trabajosPerdidos);

                if (this.storedItems >= maxStorage) {
                    trabajosPerdidos = maxStorage - this.storedItems;
                }
            } else {
                this.storedItems += trabajosPerdidos;
            }

            this.trabajosRealizados += trabajosPerdidos;
            consumirCombustibles();

            this.entity.getPersistentDataContainer().set(MinionKeys.STORED_ITEMS, PersistentDataType.INTEGER, this.storedItems);

            this.nextActionTime = currentTimeMillis + tiempoPorAccion;
            this.entity.getPersistentDataContainer().set(MinionKeys.NEXT_ACTION, PersistentDataType.LONG, this.nextActionTime);
        }
    }

    public void tick(long currentTimeMillis) {
        int maxStorage = getRealMaxStorage();

        actualizarHolograma(maxStorage);

        if (storedItems >= maxStorage && !tieneMejora("STORAGE_LINK")) {
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

    private void actualizarHolograma(int maxStorage) {
        if (holograma == null || holograma.isDead()) return;

        // 🌟 Reemplazo a Component Nativo de Paper 1.21 y Paleta HEX
        if (storedItems >= maxStorage && !tieneMejora("STORAGE_LINK")) {
            holograma.text(NexoColor.parse("&#FF5555<bold>¡CAPACIDAD MÁXIMA ALCANZADA!</bold>\n&#AAAAAAReservas: &#00E5FF" + storedItems + " / " + maxStorage));
        } else {
            String nombreBonito = type.name().replace("MINION_", "").replace("_", " ");
            holograma.text(NexoColor.parse("&#FFAA00<bold>Operario " + nombreBonito + "</bold> &#AAAAAA(Nv. " + tier + ")\n&#AAAAAAAlmacenado: &#55FF55" + storedItems + " / " + maxStorage));
        }
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
            if (this.storedItems < getRealMaxStorage()) {
                this.storedItems += 1;
                entity.getPersistentDataContainer().set(MinionKeys.STORED_ITEMS, PersistentDataType.INTEGER, this.storedItems);
            }
        }

        this.trabajosRealizados++;
        consumirCombustibles();

        Player owner = Bukkit.getPlayer(ownerId);
        if (owner != null && owner.isOnline()) {
            String blockId = type.getTargetMaterial().name();
            NexoColecciones.getPlugin(NexoColecciones.class).getCollectionManager().addProgress(owner, blockId, 1);
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
        long currentTime = System.currentTimeMillis();

        if (cachedStorage != null) {
            if (cachedStorage.getInventory().getLocation() != null &&
                    cachedStorage.getInventory().getLocation().getBlock().getState() instanceof InventoryHolder) {

                var sobrante = cachedStorage.getInventory().addItem(item);
                if (sobrante.isEmpty()) {
                    return true;
                } else {
                    cachedStorage = null;
                }
            } else {
                cachedStorage = null;
            }
        }

        if (currentTime - lastStorageCheckTime > 10000) {
            lastStorageCheckTime = currentTime;
            int[][] offsets = {{1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}};

            for (int[] offset : offsets) {
                Block b = entity.getLocation().clone().add(offset[0], 0, offset[2]).getBlock();
                if (b.getState() instanceof InventoryHolder holder) {
                    var sobrante = holder.getInventory().addItem(item);
                    if (sobrante.isEmpty()) {
                        cachedStorage = holder;
                        return true;
                    }
                }
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
        entity.setInterpolationDuration(20);
        entity.setInterpolationDelay(0);

        Transformation trans = entity.getTransformation();
        float nuevoAngulo = (System.currentTimeMillis() % 4000) / 4000f * (float) Math.PI * 2;
        trans.getLeftRotation().set(new AxisAngle4f(nuevoAngulo, new Vector3f(0, 1, 0)));
        entity.setTransformation(trans);
    }

    public ItemDisplay getEntity() { return entity; }
    public Interaction getHitbox() { return hitbox; }
    public TextDisplay getHolograma() { return holograma; }
    public UUID getOwnerId() { return ownerId; }
    public MinionType getType() { return type; }
    public int getTier() { return tier; }
    public int getStoredItems() { return storedItems; }
    public void setStoredItems(int storedItems) { this.storedItems = storedItems; }
}