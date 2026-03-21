package me.nexo.economy.bazar;

import me.nexo.core.NexoCore;
import me.nexo.economy.NexoEconomy;
import me.nexo.economy.core.NexoAccount;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

public class BazaarManager {

    private final NexoEconomy plugin;
    private final NexoCore core;

    public BazaarManager(NexoEconomy plugin) {
        this.plugin = plugin;
        this.core = NexoCore.getPlugin(NexoCore.class);
        crearTablasBazar();
    }

    private void crearTablasBazar() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = core.getDatabaseManager().getConnection();
                 Statement stmt = conn.createStatement()) {

                // Tabla de Órdenes
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS nexo_bazaar_orders (
                        order_id UUID PRIMARY KEY,
                        owner_id UUID NOT NULL,
                        order_type VARCHAR(10) NOT NULL,
                        item_id VARCHAR(64) NOT NULL,
                        amount INT NOT NULL,
                        price_per_unit DECIMAL(20,2) NOT NULL,
                        timestamp BIGINT NOT NULL
                    );
                """);
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_bazaar_item ON nexo_bazaar_orders(item_id);");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_bazaar_price ON nexo_bazaar_orders(price_per_unit);");

                // 🌟 NUEVO: Tabla de Entregas (Buzón de ítems para jugadores offline)
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS nexo_bazaar_deliveries (
                        id UUID PRIMARY KEY,
                        owner_id UUID NOT NULL,
                        item_id VARCHAR(64) NOT NULL,
                        amount INT NOT NULL
                    );
                """);

            } catch (Exception e) {
                plugin.getLogger().severe("Error creando tablas del Bazar: " + e.getMessage());
            }
        });
    }

    // ==========================================
    // 📉 CREAR ORDEN DE VENTA (SELL ORDER)
    // ==========================================
    public void crearOrdenVenta(Player player, String itemId, int amount, BigDecimal pricePerUnit) {
        Material mat = Material.matchMaterial(itemId);
        if (mat == null || !player.getInventory().contains(mat, amount)) {
            player.sendMessage("§cNo tienes suficientes ítems en tu inventario.");
            return;
        }

        quitarItems(player, mat, amount);
        guardarOrdenYEmparejar(player.getUniqueId(), BazaarOrder.OrderType.SELL, itemId, amount, pricePerUnit);
        player.sendMessage("§aHas creado una Orden de Venta por §e" + amount + "x " + mat.name() + " §aa §e🪙 " + pricePerUnit + " c/u.");
    }

    // ==========================================
    // 📈 CREAR ORDEN DE COMPRA (BUY ORDER)
    // ==========================================
    public void crearOrdenCompra(Player player, String itemId, int amount, BigDecimal pricePerUnit) {
        BigDecimal totalCost = pricePerUnit.multiply(new BigDecimal(amount));

        plugin.getEconomyManager().updateBalanceAsync(player.getUniqueId(), NexoAccount.AccountType.PLAYER, NexoAccount.Currency.COINS, totalCost, false).thenAccept(success -> {
            if (success) {
                guardarOrdenYEmparejar(player.getUniqueId(), BazaarOrder.OrderType.BUY, itemId, amount, pricePerUnit);
                player.sendMessage("§aHas creado una Orden de Compra por §e" + amount + "x " + itemId + " §apor un total de §e🪙 " + totalCost + ".");
            } else {
                player.sendMessage("§cNo tienes suficientes Monedas en tu cuenta.");
            }
        });
    }

    // ==========================================
    // ⚙️ EL MOTOR DE EMPAREJAMIENTO (MATCHING ENGINE)
    // ==========================================
    private void guardarOrdenYEmparejar(UUID ownerId, BazaarOrder.OrderType type, String itemId, int amount, BigDecimal pricePerUnit) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            UUID orderId = UUID.randomUUID();
            long timestamp = System.currentTimeMillis();

            // 1. Guardamos la nueva orden en la BD
            String insert = "INSERT INTO nexo_bazaar_orders (order_id, owner_id, order_type, item_id, amount, price_per_unit, timestamp) VALUES (CAST(? AS UUID), CAST(? AS UUID), ?, ?, ?, ?, ?)";
            try (Connection conn = core.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(insert)) {
                ps.setString(1, orderId.toString());
                ps.setString(2, ownerId.toString());
                ps.setString(3, type.name());
                ps.setString(4, itemId.toUpperCase());
                ps.setInt(5, amount);
                ps.setBigDecimal(6, pricePerUnit);
                ps.setLong(7, timestamp);
                ps.executeUpdate();
            } catch (Exception e) {
                plugin.getLogger().severe("Error guardando orden: " + e.getMessage());
                return; // Si falla, no intentamos emparejar
            }

            // 2. ¡DESPERTAMOS AL MOTOR DE EMPAREJAMIENTO!
            ejecutarMotorDeCruce(itemId);
        });
    }

    private void ejecutarMotorDeCruce(String itemId) {
        // Este motor busca las mejores compras y ventas del mismo ítem y las cruza si el precio cuadra.
        String findMatchSQL = """
            SELECT b.order_id AS buy_id, b.owner_id AS buyer_id, b.amount AS buy_amount, b.price_per_unit AS buy_price,
                   s.order_id AS sell_id, s.owner_id AS seller_id, s.amount AS sell_amount, s.price_per_unit AS sell_price
            FROM nexo_bazaar_orders b
            INNER JOIN nexo_bazaar_orders s ON b.item_id = s.item_id
            WHERE b.order_type = 'BUY' AND s.order_type = 'SELL' 
              AND b.item_id = ? 
              AND b.price_per_unit >= s.price_per_unit
            ORDER BY s.price_per_unit ASC, b.timestamp ASC
            LIMIT 1
        """;

        try (Connection conn = core.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(findMatchSQL)) {

            ps.setString(1, itemId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                // ¡HAY UN CRUCE PERFECTO!
                UUID buyId = UUID.fromString(rs.getString("buy_id"));
                UUID buyerId = UUID.fromString(rs.getString("buyer_id"));
                int buyAmount = rs.getInt("buy_amount");

                UUID sellId = UUID.fromString(rs.getString("sell_id"));
                UUID sellerId = UUID.fromString(rs.getString("seller_id"));
                int sellAmount = rs.getInt("sell_amount");

                // El precio se define por el vendedor (Maker)
                BigDecimal matchPrice = rs.getBigDecimal("sell_price");

                // Determinamos la cantidad a intercambiar (el mínimo entre lo que piden y lo que venden)
                int cantidadIntercambiada = Math.min(buyAmount, sellAmount);
                BigDecimal totalTransferencia = matchPrice.multiply(new BigDecimal(cantidadIntercambiada));

                // 💸 APLICAMOS IMPUESTO ANTI-INFLACIÓN (1% se quema)
                BigDecimal tax = totalTransferencia.multiply(new BigDecimal("0.01"));
                BigDecimal netoParaVendedor = totalTransferencia.subtract(tax);

                // 1. Damos el Dinero al Vendedor
                plugin.getEconomyManager().updateBalanceAsync(sellerId, NexoAccount.AccountType.PLAYER, NexoAccount.Currency.COINS, netoParaVendedor, true);

                // 2. Damos los Ítems al Comprador (Al buzón de Entregas)
                enviarABuzon(buyerId, itemId, cantidadIntercambiada);

                // 3. Actualizamos o borramos las órdenes de la Base de Datos
                actualizarOrden(conn, buyId, buyAmount - cantidadIntercambiada);
                actualizarOrden(conn, sellId, sellAmount - cantidadIntercambiada);

                plugin.getLogger().info("📈 [BAZAR] Cruce exitoso de " + cantidadIntercambiada + "x " + itemId + " por " + totalTransferencia + " Monedas.");

                // 🔄 Como podría haber MÁS órdenes que cruzar, llamamos al motor de nuevo recursivamente
                ejecutarMotorDeCruce(itemId);
            }

        } catch (Exception e) {
            plugin.getLogger().severe("Error en el Matching Engine: " + e.getMessage());
        }
    }

    private void actualizarOrden(Connection conn, UUID orderId, int remainingAmount) throws Exception {
        if (remainingAmount <= 0) {
            String delete = "DELETE FROM nexo_bazaar_orders WHERE order_id = CAST(? AS UUID)";
            try (PreparedStatement ps = conn.prepareStatement(delete)) {
                ps.setString(1, orderId.toString());
                ps.executeUpdate();
            }
        } else {
            String update = "UPDATE nexo_bazaar_orders SET amount = ? WHERE order_id = CAST(? AS UUID)";
            try (PreparedStatement ps = conn.prepareStatement(update)) {
                ps.setInt(1, remainingAmount);
                ps.setString(2, orderId.toString());
                ps.executeUpdate();
            }
        }
    }

    private void enviarABuzon(UUID ownerId, String itemId, int amount) {
        String insert = "INSERT INTO nexo_bazaar_deliveries (id, owner_id, item_id, amount) VALUES (CAST(? AS UUID), CAST(? AS UUID), ?, ?)";
        try (Connection conn = core.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(insert)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, ownerId.toString());
            ps.setString(3, itemId);
            ps.setInt(4, amount);
            ps.executeUpdate();

            // Avisar si el jugador está online
            Player p = Bukkit.getPlayer(ownerId);
            if (p != null) {
                p.sendMessage("§8========================================");
                p.sendMessage("§a📦 ¡Una orden del Bazar se ha completado!");
                p.sendMessage("§7Usa §e/bazar claim §7para recoger tus ítems o monedas.");
                p.sendMessage("§8========================================");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error entregando ítem al buzón: " + e.getMessage());
        }
    }

    private void quitarItems(Player player, Material mat, int amountToRemove) {
        int removed = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == mat) {
                if (removed + item.getAmount() <= amountToRemove) {
                    removed += item.getAmount();
                    item.setAmount(0);
                } else {
                    int toTake = amountToRemove - removed;
                    item.setAmount(item.getAmount() - toTake);
                    removed += toTake;
                    break;
                }
            }
        }
    }

    // ==========================================
    // 📦 RECLAMAR BUZÓN DE ENTREGAS
    // ==========================================
    public void reclamarBuzon(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String select = "SELECT id, item_id, amount FROM nexo_bazaar_deliveries WHERE owner_id = CAST(? AS UUID)";
            try (Connection conn = core.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(select)) {

                ps.setString(1, player.getUniqueId().toString());
                ResultSet rs = ps.executeQuery();
                boolean tieneItems = false;

                while (rs.next()) {
                    tieneItems = true;
                    String deliveryId = rs.getString("id");
                    String itemId = rs.getString("item_id");
                    int amount = rs.getInt("amount");

                    // Borramos el ítem de la BD (para que no lo dupliquen)
                    String delete = "DELETE FROM nexo_bazaar_deliveries WHERE id = CAST(? AS UUID)";
                    try (PreparedStatement delPs = conn.prepareStatement(delete)) {
                        delPs.setString(1, deliveryId);
                        delPs.executeUpdate();
                    }

                    // Le damos el ítem al jugador en el hilo principal
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        Material mat = Material.matchMaterial(itemId);
                        if (mat != null) {
                            ItemStack item = new ItemStack(mat, amount);
                            if (player.getInventory().firstEmpty() == -1) {
                                player.getWorld().dropItemNaturally(player.getLocation(), item);
                            } else {
                                player.getInventory().addItem(item);
                            }
                            player.sendMessage("§a📦 Has recogido del buzón: §e" + amount + "x " + mat.name());
                        }
                    });
                }

                if (!tieneItems) {
                    Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage("§cTu buzón de entregas del Bazar está vacío."));
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error reclamando buzón: " + e.getMessage());
            }
        });
    }

}