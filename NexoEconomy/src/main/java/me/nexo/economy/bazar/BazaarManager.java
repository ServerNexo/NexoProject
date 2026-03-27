package me.nexo.economy.bazar;

import me.nexo.core.NexoCore;
import me.nexo.core.utils.NexoColor;
import me.nexo.economy.NexoEconomy;
import me.nexo.economy.core.NexoAccount;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BazaarManager {

    private final NexoEconomy plugin;
    private final NexoCore core;

    // 🎨 PALETA HEX - CONSTANTES INDUSTRIALES
    private static final String ERR_NO_ITEMS = "&#ff4b2b[!] Stock insuficiente en tu inventario local para cubrir la orden.";
    private static final String MSG_SELL_ORDER = "&#a8ff78[✓] Orden de Venta (ASK) generada: &#fbd72b%amount%x %item% &#434343a &#fbd72b🪙 %price% c/u.";
    private static final String MSG_BUY_ORDER = "&#a8ff78[✓] Orden de Compra (BID) generada: &#fbd72b%amount%x %item% &#434343(Total: &#fbd72b🪙 %total%&#434343).";
    private static final String ERR_NO_COINS = "&#ff4b2b[!] Fondos insuficientes para respaldar la orden de compra.";

    private static final String BC_DIVIDER = "&#434343=======================================";
    private static final String MSG_DELIVERY_TITLE = "&#a8ff78<bold>📦 ¡CONTRATO DE BAZAR COMPLETADO!</bold>";
    private static final String MSG_DELIVERY_DESC = "&#434343Los activos han sido depositados. Usa &#fbd72b/bazar claim &#434343para extraerlos.";

    private static final String MSG_CLAIM_SUCCESS = "&#a8ff78[✓] Extracción corporativa exitosa: &#fbd72b%amount%x %item%";
    private static final String MSG_CLAIM_COINS = "&#a8ff78[✓] Reembolso corporativo exitoso: &#fbd72b🪙 %coins%";
    private static final String ERR_CLAIM_EMPTY = "&#ff4b2b[!] Tu buzón corporativo se encuentra actualmente vacío.";

    // 🌟 Mensaje de Censura del Vacío
    private static final String ERR_NO_COLLECTION_LEVEL = "&#ff4b2b[!] Transacción Denegada: &#e0e0e0Debes alcanzar el &#fbd72bNivel 1 &#e0e0e0en la colección de este material antes de poder comerciarlo.";

    public BazaarManager(NexoEconomy plugin) {
        this.plugin = plugin;
        this.core = NexoCore.getPlugin(NexoCore.class);
        crearTablasBazar();
    }

    private void crearTablasBazar() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = core.getDatabaseManager().getConnection();
                 Statement stmt = conn.createStatement()) {

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS nexo_bazaar_orders (
                        order_id SERIAL PRIMARY KEY, -- Usar SERIAL (Autoincremental) facilita la UI de Bedrock
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

                // 🌟 FASE 2: Hemos añadido la columna 'coins' para los reembolsos
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS nexo_bazaar_deliveries (
                        id UUID PRIMARY KEY,
                        owner_id UUID NOT NULL,
                        item_id VARCHAR(64) NOT NULL,
                        amount INT NOT NULL,
                        coins DECIMAL(20,2) DEFAULT 0
                    );
                """);

            } catch (Exception e) {
                plugin.getLogger().severe("Error creando tablas del Bazar: " + e.getMessage());
            }
        });
    }

    // ==========================================
    // 🦇 INTEGRACIÓN CON NEXOCOLECCIONES
    // ==========================================
    private boolean tieneNivelComercial(Player player, String itemId) {
        if (Bukkit.getPluginManager().getPlugin("NexoColecciones") == null) return true;

        try {
            me.nexo.colecciones.NexoColecciones colPlugin = me.nexo.colecciones.NexoColecciones.getPlugin(me.nexo.colecciones.NexoColecciones.class);
            me.nexo.colecciones.colecciones.CollectionManager colManager = colPlugin.getCollectionManager();

            me.nexo.colecciones.data.CollectionItem itemData = colManager.getItemGlobal(itemId);
            if (itemData == null) return true;

            me.nexo.colecciones.colecciones.CollectionProfile profile = colManager.getProfile(player.getUniqueId());
            if (profile == null) return false;

            int nivel = colManager.calcularNivel(itemData, profile.getProgress(itemId));
            return nivel >= 1;

        } catch (Exception e) {
            return true;
        }
    }

    // ==========================================
    // 📉 CREAR ORDEN DE VENTA (SELL ORDER)
    // ==========================================
    public void crearOrdenVenta(Player player, String itemId, int amount, BigDecimal pricePerUnit) {
        if (!tieneNivelComercial(player, itemId)) {
            player.sendMessage(NexoColor.parse(ERR_NO_COLLECTION_LEVEL));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        Material mat = Material.matchMaterial(itemId);
        if (mat == null || !player.getInventory().contains(mat, amount)) {
            player.sendMessage(NexoColor.parse(ERR_NO_ITEMS));
            return;
        }

        quitarItems(player, mat, amount);
        guardarOrdenYEmparejar(player.getUniqueId(), BazaarOrder.OrderType.SELL, itemId, amount, pricePerUnit);
        player.sendMessage(NexoColor.parse(MSG_SELL_ORDER.replace("%amount%", String.valueOf(amount)).replace("%item%", mat.name()).replace("%price%", pricePerUnit.toString())));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }

    // ==========================================
    // 📈 CREAR ORDEN DE COMPRA (BUY ORDER)
    // ==========================================
    public void crearOrdenCompra(Player player, String itemId, int amount, BigDecimal pricePerUnit) {
        if (!tieneNivelComercial(player, itemId)) {
            player.sendMessage(NexoColor.parse(ERR_NO_COLLECTION_LEVEL));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        BigDecimal totalCost = pricePerUnit.multiply(new BigDecimal(amount));

        plugin.getEconomyManager().updateBalanceAsync(player.getUniqueId(), NexoAccount.AccountType.PLAYER, NexoAccount.Currency.COINS, totalCost, false).thenAccept(success -> {
            if (success) {
                guardarOrdenYEmparejar(player.getUniqueId(), BazaarOrder.OrderType.BUY, itemId, amount, pricePerUnit);
                player.sendMessage(NexoColor.parse(MSG_BUY_ORDER.replace("%amount%", String.valueOf(amount)).replace("%item%", itemId).replace("%total%", totalCost.toString())));
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            } else {
                player.sendMessage(NexoColor.parse(ERR_NO_COINS));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        });
    }

    // ==========================================
    // ⚙️ EL MOTOR DE EMPAREJAMIENTO (MATCHING ENGINE)
    // ==========================================
    private void guardarOrdenYEmparejar(UUID ownerId, BazaarOrder.OrderType type, String itemId, int amount, BigDecimal pricePerUnit) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            long timestamp = System.currentTimeMillis();
            String insert = "INSERT INTO nexo_bazaar_orders (owner_id, order_type, item_id, amount, price_per_unit, timestamp) VALUES (CAST(? AS UUID), ?, ?, ?, ?, ?)";

            try (Connection conn = core.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(insert)) {
                ps.setString(1, ownerId.toString());
                ps.setString(2, type.name());
                ps.setString(3, itemId.toUpperCase());
                ps.setInt(4, amount);
                ps.setBigDecimal(5, pricePerUnit);
                ps.setLong(6, timestamp);
                ps.executeUpdate();
            } catch (Exception e) {
                plugin.getLogger().severe("Error guardando orden: " + e.getMessage());
                return;
            }

            ejecutarMotorDeCruce(itemId);
        });
    }

    private void ejecutarMotorDeCruce(String itemId) {
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
                int buyId = rs.getInt("buy_id");
                UUID buyerId = UUID.fromString(rs.getString("buyer_id"));
                int buyAmount = rs.getInt("buy_amount");

                int sellId = rs.getInt("sell_id");
                UUID sellerId = UUID.fromString(rs.getString("seller_id"));
                int sellAmount = rs.getInt("sell_amount");

                BigDecimal matchPrice = rs.getBigDecimal("sell_price");
                int cantidadIntercambiada = Math.min(buyAmount, sellAmount);
                BigDecimal totalTransferencia = matchPrice.multiply(new BigDecimal(cantidadIntercambiada));

                BigDecimal tax = totalTransferencia.multiply(new BigDecimal("0.01"));
                BigDecimal netoParaVendedor = totalTransferencia.subtract(tax);

                plugin.getEconomyManager().updateBalanceAsync(sellerId, NexoAccount.AccountType.PLAYER, NexoAccount.Currency.COINS, netoParaVendedor, true);
                enviarABuzon(buyerId, itemId, cantidadIntercambiada, BigDecimal.ZERO); // Entregamos Ítems

                actualizarOrden(conn, buyId, buyAmount - cantidadIntercambiada);
                actualizarOrden(conn, sellId, sellAmount - cantidadIntercambiada);

                plugin.getLogger().info("📈 [BAZAR] Cruce exitoso de " + cantidadIntercambiada + "x " + itemId + " por " + totalTransferencia + " Monedas.");
                ejecutarMotorDeCruce(itemId); // Recursivo hasta que no haya más cruces posibles
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error en el Matching Engine: " + e.getMessage());
        }
    }

    private void actualizarOrden(Connection conn, int orderId, int remainingAmount) throws Exception {
        if (remainingAmount <= 0) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM nexo_bazaar_orders WHERE order_id = ?")) {
                ps.setInt(1, orderId);
                ps.executeUpdate();
            }
        } else {
            try (PreparedStatement ps = conn.prepareStatement("UPDATE nexo_bazaar_orders SET amount = ? WHERE order_id = ?")) {
                ps.setInt(1, remainingAmount);
                ps.setInt(2, orderId);
                ps.executeUpdate();
            }
        }
    }

    private void enviarABuzon(UUID ownerId, String itemId, int amount, BigDecimal coins) {
        String insert = "INSERT INTO nexo_bazaar_deliveries (id, owner_id, item_id, amount, coins) VALUES (CAST(? AS UUID), CAST(? AS UUID), ?, ?, ?)";
        try (Connection conn = core.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(insert)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, ownerId.toString());
            ps.setString(3, itemId);
            ps.setInt(4, amount);
            ps.setBigDecimal(5, coins);
            ps.executeUpdate();

            Player p = Bukkit.getPlayer(ownerId);
            if (p != null && amount > 0) { // Solo avisar si son ítems de cruce (No reembolsos silenciosos)
                p.sendMessage(NexoColor.parse(BC_DIVIDER));
                p.sendMessage(NexoColor.parse(MSG_DELIVERY_TITLE));
                p.sendMessage(NexoColor.parse(MSG_DELIVERY_DESC));
                p.sendMessage(NexoColor.parse(BC_DIVIDER));
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.2f);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error entregando al buzón: " + e.getMessage());
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
            String select = "SELECT id, item_id, amount, coins FROM nexo_bazaar_deliveries WHERE owner_id = CAST(? AS UUID)";
            try (Connection conn = core.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(select)) {

                ps.setString(1, player.getUniqueId().toString());
                ResultSet rs = ps.executeQuery();
                boolean tieneCosas = false;

                while (rs.next()) {
                    tieneCosas = true;
                    String deliveryId = rs.getString("id");
                    String itemId = rs.getString("item_id");
                    int amount = rs.getInt("amount");
                    BigDecimal coins = rs.getBigDecimal("coins");

                    String delete = "DELETE FROM nexo_bazaar_deliveries WHERE id = CAST(? AS UUID)";
                    try (PreparedStatement delPs = conn.prepareStatement(delete)) {
                        delPs.setString(1, deliveryId);
                        delPs.executeUpdate();
                    }

                    if (itemId.equals("COINS") || coins.compareTo(BigDecimal.ZERO) > 0) {
                        // Reclama DINERO (Reembolsos de compras canceladas)
                        plugin.getEconomyManager().updateBalanceAsync(player.getUniqueId(), NexoAccount.AccountType.PLAYER, NexoAccount.Currency.COINS, coins, true);
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            player.sendMessage(NexoColor.parse(MSG_CLAIM_COINS.replace("%coins%", coins.toString())));
                            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
                        });
                    } else {
                        // Reclama ÍTEMS (Compras exitosas o ventas canceladas)
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            Material mat = Material.matchMaterial(itemId);
                            if (mat != null) {
                                ItemStack item = new ItemStack(mat, amount);
                                if (player.getInventory().firstEmpty() == -1) {
                                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                                } else {
                                    player.getInventory().addItem(item);
                                }
                                player.sendMessage(NexoColor.parse(MSG_CLAIM_SUCCESS.replace("%amount%", String.valueOf(amount)).replace("%item%", mat.name())));
                                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
                            }
                        });
                    }
                }

                if (!tieneCosas) {
                    Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(NexoColor.parse(ERR_CLAIM_EMPTY)));
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error reclamando buzón: " + e.getMessage());
            }
        });
    }

    // ==========================================
    // 📊 FASE 1: ESTADÍSTICAS EN VIVO (ORDER BOOK Y FLIPPING)
    // ==========================================
    public BigDecimal getMejorPrecioCompra(String itemId) {
        try (Connection conn = core.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT MAX(price_per_unit) FROM nexo_bazaar_orders WHERE item_id = ? AND order_type = 'BUY'")) {
            ps.setString(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getBigDecimal(1) != null) return rs.getBigDecimal(1);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return BigDecimal.ZERO;
    }

    public BigDecimal getMejorPrecioVenta(String itemId) {
        try (Connection conn = core.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT MIN(price_per_unit) FROM nexo_bazaar_orders WHERE item_id = ? AND order_type = 'SELL'")) {
            ps.setString(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getBigDecimal(1) != null) return rs.getBigDecimal(1);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return BigDecimal.ZERO;
    }

    public int getVolumenOrdenes(String itemId, String type) {
        try (Connection conn = core.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT SUM(amount) FROM nexo_bazaar_orders WHERE item_id = ? AND order_type = ?")) {
            ps.setString(1, itemId);
            ps.setString(2, type);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    // ==========================================
    // ❌ FASE 2: GESTIÓN Y CANCELACIÓN DE ÓRDENES
    // ==========================================
    public static class ActiveOrderDTO {
        public int id; public String itemId; public int amount; public BigDecimal price; public String type;
        public ActiveOrderDTO(int id, String itemId, int amount, BigDecimal price, String type) {
            this.id = id; this.itemId = itemId; this.amount = amount; this.price = price; this.type = type;
        }
    }

    public List<ActiveOrderDTO> getMisOrdenes(UUID ownerId) {
        List<ActiveOrderDTO> orders = new ArrayList<>();
        try (Connection conn = core.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT order_id, item_id, amount, price_per_unit, order_type FROM nexo_bazaar_orders WHERE owner_id = CAST(? AS UUID) ORDER BY timestamp DESC")) {
            ps.setString(1, ownerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    orders.add(new ActiveOrderDTO(
                            rs.getInt("order_id"), rs.getString("item_id"), rs.getInt("amount"),
                            rs.getBigDecimal("price_per_unit"), rs.getString("order_type")
                    ));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return orders;
    }

    public void cancelarOrden(Player player, int orderId) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = core.getDatabaseManager().getConnection()) {

                PreparedStatement psSearch = conn.prepareStatement("SELECT item_id, amount, price_per_unit, order_type FROM nexo_bazaar_orders WHERE order_id = ? AND owner_id = CAST(? AS UUID)");
                psSearch.setInt(1, orderId);
                psSearch.setString(2, player.getUniqueId().toString());
                ResultSet rs = psSearch.executeQuery();

                if (rs.next()) {
                    String itemId = rs.getString("item_id");
                    int amount = rs.getInt("amount");
                    BigDecimal price = rs.getBigDecimal("price_per_unit");
                    String type = rs.getString("order_type");

                    PreparedStatement psDelete = conn.prepareStatement("DELETE FROM nexo_bazaar_orders WHERE order_id = ?");
                    psDelete.setInt(1, orderId);
                    psDelete.executeUpdate();

                    if (type.equals("SELL")) {
                        enviarABuzon(player.getUniqueId(), itemId, amount, BigDecimal.ZERO);
                    } else {
                        BigDecimal totalCoins = price.multiply(new BigDecimal(amount));
                        enviarABuzon(player.getUniqueId(), "COINS", 0, totalCoins);
                    }

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.sendMessage(NexoColor.parse("&#a8ff78[✓] Orden cancelada. Activos devueltos a tu Buzón de Entregas."));
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);
                    });

                } else {
                    Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(NexoColor.parse("&#ff4b2b[!] Esta orden ya no existe o se completó totalmente.")));
                }
            } catch (Exception e) { e.printStackTrace(); }
        });
    }
}