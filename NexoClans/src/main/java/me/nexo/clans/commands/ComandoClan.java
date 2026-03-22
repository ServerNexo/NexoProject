package me.nexo.clans.commands;

import me.nexo.clans.NexoClans;
import me.nexo.clans.core.NexoClan;
import me.nexo.core.NexoCore;
import me.nexo.core.user.NexoUser;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

public class ComandoClan implements CommandExecutor {

    private final NexoClans plugin;

    public ComandoClan(NexoClans plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        NexoUser user = NexoCore.getPlugin(NexoCore.class).getUserManager().getUserOrNull(player.getUniqueId());

        if (user == null) { player.sendMessage("§cTus datos aún están cargando."); return true; }

        if (args.length == 0) {
            if (user.hasClan()) {
                Optional<NexoClan> clanOpt = plugin.getClanManager().getClanFromCache(user.getClanId());
                clanOpt.ifPresentOrElse(
                        clan -> me.nexo.clans.menu.ClanMenu.abrirMenu(player, clan, user),
                        () -> player.sendMessage("§cCargando datos...")
                );
            } else {
                player.sendMessage("§cNo tienes clan. Usa /clan create <tag> <nombre>");
            }
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("create")) {
            if (user.hasClan()) { player.sendMessage("§cYa perteneces a un clan."); return true; }
            if (args.length < 3) { player.sendMessage("§cUso: /clan create <Tag> <Nombre>"); return true; }
            String tag = args[1].toUpperCase();
            String nombre = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
            plugin.getClanManager().crearClanAsync(player, user, tag, nombre);
            return true;
        }

        if (subCommand.equals("invite")) {
            if (!user.hasClan() || (!user.getClanRole().equals("LIDER") && !user.getClanRole().equals("OFICIAL"))) return true;
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target != null) plugin.getClanManager().getClanFromCache(user.getClanId()).ifPresent(clan -> plugin.getClanManager().invitarJugador(player, target, clan));
            return true;
        }

        if (subCommand.equals("join")) {
            UUID inv = plugin.getClanManager().getInvitacionPendiente(player);
            if (inv != null) plugin.getClanManager().loadClanAsync(inv, clan -> plugin.getClanManager().unirseClanAsync(player, user, clan));
            return true;
        }

        if (subCommand.equals("leave")) {
            if (user.hasClan() && !user.getClanRole().equals("LIDER")) plugin.getClanManager().abandonarClanAsync(player, user);
            return true;
        }

        if (subCommand.equals("ff") || subCommand.equals("friendlyfire")) {
            if (user.hasClan() && (user.getClanRole().equals("LIDER") || user.getClanRole().equals("OFICIAL"))) {
                plugin.getClanManager().getClanFromCache(user.getClanId()).ifPresent(clan -> plugin.getClanManager().toggleFriendlyFireAsync(clan, player, !clan.isFriendlyFire()));
            }
            return true;
        }

        if (subCommand.equals("kick")) {
            if (!user.hasClan() || (!user.getClanRole().equals("LIDER") && !user.getClanRole().equals("OFICIAL"))) return true;
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target != null) {
                NexoUser tUser = NexoCore.getPlugin(NexoCore.class).getUserManager().getUserOrNull(target.getUniqueId());
                if (tUser != null && tUser.getClanId().equals(user.getClanId())) plugin.getClanManager().expulsarJugadorAsync(player, target, tUser);
            }
            return true;
        }

        if (subCommand.equals("disband")) {
            if (user.hasClan() && user.getClanRole().equals("LIDER")) plugin.getClanManager().disolverClanAsync(player, user, user.getClanId());
            return true;
        }

        // 🌟 NUEVO: /clan deposit <cantidad>
        if (subCommand.equals("deposit")) {
            if (!user.hasClan()) { player.sendMessage("§cNo perteneces a ningún clan."); return true; }
            if (args.length < 2) { player.sendMessage("§cUso correcto: /clan deposit <cantidad>"); return true; }

            try {
                java.math.BigDecimal amount = new java.math.BigDecimal(args[1]);
                if (amount.compareTo(java.math.BigDecimal.ZERO) <= 0) {
                    player.sendMessage("§cLa cantidad debe ser mayor a 0."); return true;
                }

                me.nexo.economy.NexoEconomy eco = me.nexo.economy.NexoEconomy.getPlugin(me.nexo.economy.NexoEconomy.class);

                // 1. Transacción Atómica: Le quitamos las monedas al jugador
                eco.getEconomyManager().updateBalanceAsync(player.getUniqueId(), me.nexo.economy.core.NexoAccount.AccountType.PLAYER, me.nexo.economy.core.NexoAccount.Currency.COINS, amount, false).thenAccept(success -> {
                    if (success) {
                        // 2. Si se pudo, se lo sumamos al clan
                        plugin.getClanManager().getClanFromCache(user.getClanId()).ifPresent(clan -> {
                            clan.depositMoney(amount.doubleValue());

                            // Guardamos en la base de datos de clanes
                            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                                String sql = "UPDATE nexo_clans SET bank_balance = ? WHERE id = CAST(? AS UUID)";
                                try (java.sql.Connection conn = NexoCore.getPlugin(NexoCore.class).getDatabaseManager().getConnection();
                                     java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                                    ps.setBigDecimal(1, clan.getBankBalance());
                                    ps.setString(2, clan.getId().toString());
                                    ps.executeUpdate();
                                } catch (Exception e) {}
                            });
                            player.sendMessage("§aHas depositado §e🪙 " + amount + " §aal banco de tu clan.");
                        });
                    } else {
                        player.sendMessage("§cNo tienes suficientes Monedas en tu billetera personal.");
                    }
                });
            } catch (Exception e) { player.sendMessage("§cCantidad inválida."); }
            return true;
        }

        // 🌟 NUEVO: /clan withdraw <cantidad>
        if (subCommand.equals("withdraw")) {
            if (!user.hasClan()) { player.sendMessage("§cNo perteneces a ningún clan."); return true; }
            if (!user.getClanRole().equals("LIDER") && !user.getClanRole().equals("OFICIAL")) {
                player.sendMessage("§cSolo el Líder y los Oficiales pueden retirar fondos del banco."); return true;
            }
            if (args.length < 2) { player.sendMessage("§cUso correcto: /clan withdraw <cantidad>"); return true; }

            try {
                java.math.BigDecimal amount = new java.math.BigDecimal(args[1]);
                if (amount.compareTo(java.math.BigDecimal.ZERO) <= 0) {
                    player.sendMessage("§cLa cantidad debe ser mayor a 0."); return true;
                }

                plugin.getClanManager().getClanFromCache(user.getClanId()).ifPresent(clan -> {
                    if (!clan.hasEnoughMoney(amount.doubleValue())) {
                        player.sendMessage("§cEl banco del clan no tiene suficientes fondos."); return;
                    }

                    // 1. Quitamos dinero del clan
                    clan.withdrawMoney(amount.doubleValue());

                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        String sql = "UPDATE nexo_clans SET bank_balance = ? WHERE id = CAST(? AS UUID)";
                        try (java.sql.Connection conn = NexoCore.getPlugin(NexoCore.class).getDatabaseManager().getConnection();
                             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                            ps.setBigDecimal(1, clan.getBankBalance());
                            ps.setString(2, clan.getId().toString());
                            ps.executeUpdate();
                        } catch (Exception e) {}
                    });

                    // 2. Transacción Atómica: Se lo damos al jugador
                    me.nexo.economy.NexoEconomy eco = me.nexo.economy.NexoEconomy.getPlugin(me.nexo.economy.NexoEconomy.class);
                    eco.getEconomyManager().updateBalanceAsync(player.getUniqueId(), me.nexo.economy.core.NexoAccount.AccountType.PLAYER, me.nexo.economy.core.NexoAccount.Currency.COINS, amount, true);

                    player.sendMessage("§aHas retirado §e🪙 " + amount + " §adel banco del clan.");
                });
            } catch (Exception e) { player.sendMessage("§cCantidad inválida."); }
            return true;
        }

        // 🌟 NUEVO: /clan sethome
        if (subCommand.equals("sethome")) {
            if (!user.hasClan() || !user.getClanRole().equals("LIDER")) {
                player.sendMessage("§cSolo el Líder puede establecer la base del Monolito.");
                return true;
            }
            plugin.getClanManager().getClanFromCache(user.getClanId()).ifPresent(clan -> {
                plugin.getClanManager().setClanHomeAsync(clan, player, player.getLocation());
            });
            return true;
        }

        // 🌟 NUEVO: /clan home
        if (subCommand.equals("home")) {
            if (!user.hasClan()) {
                player.sendMessage("§cNo perteneces a ningún clan.");
                return true;
            }
            plugin.getClanManager().getClanFromCache(user.getClanId()).ifPresent(clan -> {
                if (clan.getPublicHome() == null || clan.getPublicHome().isEmpty()) {
                    player.sendMessage("§cTu clan aún no ha establecido una base. Dile al líder que use /clan sethome.");
                    return;
                }
                try {
                    String[] parts = clan.getPublicHome().split(";");
                    World w = Bukkit.getWorld(parts[0]);
                    double x = Double.parseDouble(parts[1]);
                    double y = Double.parseDouble(parts[2]);
                    double z = Double.parseDouble(parts[3]);
                    float yaw = Float.parseFloat(parts[4]);
                    float pitch = Float.parseFloat(parts[5]);
                    player.teleport(new Location(w, x, y, z, yaw, pitch));
                    player.sendMessage("§aTeletransportado a la base del clan. 🏛️");
                } catch (Exception e) {
                    player.sendMessage("§cError leyendo las coordenadas de la base.");
                }
            });
            return true;
        }

        // 🌟 NUEVO: /clan tribute (Sacrificar ítems por EXP)
        if (subCommand.equals("tribute") || subCommand.equals("tributo")) {
            if (!user.hasClan()) { player.sendMessage("§cNo perteneces a ningún clan."); return true; }

            org.bukkit.inventory.ItemStack itemEnMano = player.getInventory().getItemInMainHand();
            if (itemEnMano == null || itemEnMano.getType() == org.bukkit.Material.AIR) {
                player.sendMessage("§cDebes tener un ítem en la mano para ofrecerlo como tributo.");
                return true;
            }

            // Calculamos cuánta XP da el ítem (Puedes balancear esto después)
            long expPorItem = 1;
            String tipo = itemEnMano.getType().name();
            if (tipo.contains("DIAMOND") || tipo.contains("EMERALD")) expPorItem = 50;
            if (tipo.contains("IRON") || tipo.contains("GOLD")) expPorItem = 10;
            if (itemEnMano.hasItemMeta() && itemEnMano.getItemMeta().hasDisplayName()) expPorItem = 100; // Ítems de NexoItems o Custom

            long expTotal = expPorItem * itemEnMano.getAmount();

            plugin.getClanManager().getClanFromCache(user.getClanId()).ifPresent(clan -> {
                boolean subioNivel = clan.addMonolithExp(expTotal);
                player.getInventory().setItemInMainHand(null); // Consumimos el ítem

                player.sendMessage("§d🔮 Has sacrificado tus ítems y el Monolito ha ganado §5" + expTotal + " EXP§d.");

                if (subioNivel) {
                    Bukkit.broadcastMessage("§8========================================");
                    Bukkit.broadcastMessage("§6§l🏛️ ¡EL CLAN " + clan.getName() + " HA EVOLUCIONADO!");
                    Bukkit.broadcastMessage("§eSu Monolito ha alcanzado el Nivel §a" + clan.getMonolithLevel() + "§e.");
                    Bukkit.broadcastMessage("§8========================================");
                }

                // Guardamos en la BD asíncronamente
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    String sql = "UPDATE nexo_clans SET monolith_exp = ?, monolith_level = ? WHERE id = CAST(? AS UUID)";
                    try (java.sql.Connection conn = NexoCore.getPlugin(NexoCore.class).getDatabaseManager().getConnection();
                         java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setLong(1, clan.getMonolithExp());
                        ps.setInt(2, clan.getMonolithLevel());
                        ps.setString(3, clan.getId().toString());
                        ps.executeUpdate();
                    } catch (Exception e) {}
                });
            });
            return true;
        }

        player.sendMessage("§7Sub-comandos: §ecreate, invite, join, leave, ff, kick, disband, sethome, home, deposit, withdraw, tribute.");
        return true;
    }
}