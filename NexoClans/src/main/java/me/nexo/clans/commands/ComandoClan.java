package me.nexo.clans.commands;

import me.nexo.clans.NexoClans;
import me.nexo.clans.core.NexoClan;
import me.nexo.core.NexoCore;
import me.nexo.core.user.NexoUser;
import me.nexo.core.utils.NexoColor;
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

    // 🎨 PALETA HEX - CONSTANTES DE TEXTO (Errores)
    private static final String ERR_DATA_LOADING = "&#ff4b2bTus datos aún están cargando. Aguarda un instante.";
    private static final String ERR_NO_CLAN = "&#ff4b2bNo perteneces a ningún clan. Usa /clan create <tag> <nombre>";
    private static final String ERR_ALREADY_IN_CLAN = "&#ff4b2bYa perteneces a una estructura de clan.";
    private static final String ERR_CREATE_USAGE = "&#ff4b2bUso industrial: /clan create <Tag> <Nombre>";
    private static final String ERR_DEPOSIT_USAGE = "&#ff4b2bUso logístico: /clan deposit <cantidad>";
    private static final String ERR_WITHDRAW_USAGE = "&#ff4b2bUso logístico: /clan withdraw <cantidad>";
    private static final String ERR_INVALID_AMOUNT = "&#ff4b2bLa cantidad monetaria debe ser mayor a 0.";
    private static final String ERR_INSUFFICIENT_FUNDS = "&#ff4b2bNo tienes suficientes fondos en tu cuenta personal.";
    private static final String ERR_CLAN_FUNDS = "&#ff4b2bEl banco del clan no tiene los fondos requeridos.";
    private static final String ERR_NO_PERM_OFFICIAL = "&#ff4b2bAcceso Denegado: Requiere rango de LÍDER u OFICIAL.";
    private static final String ERR_NO_PERM_LEADER = "&#ff4b2bAcceso Denegado: Requiere rango de LÍDER.";
    private static final String ERR_NO_HOME = "&#ff4b2bFallo de red: El clan no posee un Monolito Base (/clan sethome).";
    private static final String ERR_HOME_COORD = "&#ff4b2bError crítico leyendo las coordenadas del servidor.";
    private static final String ERR_NO_ITEM = "&#ff4b2bDebes sostener un material físico para iniciar la extracción.";

    // 🎨 PALETA HEX - CONSTANTES DE TEXTO (Éxito e Info)
    private static final String MSG_LOADING = "&#fbd72bCargando interfaz de clan...";
    private static final String MSG_DEPOSIT_SUCCESS = "&#a8ff78Transferencia completada. Depositaste &#fbd72b🪙 %amount% &#a8ff78al banco del clan.";
    private static final String MSG_WITHDRAW_SUCCESS = "&#a8ff78Retiro autorizado. Obtuviste &#fbd72b🪙 %amount% &#a8ff78del banco del clan.";
    private static final String MSG_HOME_SUCCESS = "&#00fbff[✓] Enlace establecido. Bienvenido a la Base. 🏛️";
    private static final String MSG_TRIBUTE_SUCCESS = "&#00fbff[✓] Extracción completada. El Monolito ha absorbido &#a8ff78%exp% EXP&#00fbff.";
    private static final String MSG_HELP = "&#434343Sistemas de Comando: &#fbd72bcreate, invite, join, leave, ff, kick, disband, sethome, home, deposit, withdraw, tribute.";

    // 🎨 ALERTAS GLOBALES
    private static final String BC_DIVIDER = "&#434343========================================";
    private static final String BC_LEVEL_UP_TITLE = "&#fbd72b<bold>🏛️ ¡SISTEMA EVOLUCIONADO: %clan%!</bold>";
    private static final String BC_LEVEL_UP_DESC = "&#e0e0e0Su Monolito industrial ha alcanzado el Nivel &#a8ff78%level%&#e0e0e0.";

    public ComandoClan(NexoClans plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        NexoUser user = NexoCore.getPlugin(NexoCore.class).getUserManager().getUserOrNull(player.getUniqueId());

        if (user == null) { player.sendMessage(NexoColor.parse(ERR_DATA_LOADING)); return true; }

        if (args.length == 0) {
            if (user.hasClan()) {
                Optional<NexoClan> clanOpt = plugin.getClanManager().getClanFromCache(user.getClanId());
                clanOpt.ifPresentOrElse(
                        clan -> me.nexo.clans.menu.ClanMenu.abrirMenu(player, clan, user),
                        () -> player.sendMessage(NexoColor.parse(MSG_LOADING))
                );
            } else {
                player.sendMessage(NexoColor.parse(ERR_NO_CLAN));
            }
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("create")) {
            if (user.hasClan()) { player.sendMessage(NexoColor.parse(ERR_ALREADY_IN_CLAN)); return true; }
            if (args.length < 3) { player.sendMessage(NexoColor.parse(ERR_CREATE_USAGE)); return true; }
            String tag = args[1].toUpperCase();
            String nombre = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
            plugin.getClanManager().crearClanAsync(player, user, tag, nombre);
            return true;
        }

        if (subCommand.equals("invite")) {
            if (!user.hasClan() || (!user.getClanRole().equals("LIDER") && !user.getClanRole().equals("OFICIAL"))) {
                player.sendMessage(NexoColor.parse(ERR_NO_PERM_OFFICIAL)); return true;
            }
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
            } else {
                player.sendMessage(NexoColor.parse(ERR_NO_PERM_OFFICIAL));
            }
            return true;
        }

        if (subCommand.equals("kick")) {
            if (!user.hasClan() || (!user.getClanRole().equals("LIDER") && !user.getClanRole().equals("OFICIAL"))) {
                player.sendMessage(NexoColor.parse(ERR_NO_PERM_OFFICIAL)); return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target != null) {
                NexoUser tUser = NexoCore.getPlugin(NexoCore.class).getUserManager().getUserOrNull(target.getUniqueId());
                if (tUser != null && tUser.getClanId().equals(user.getClanId())) plugin.getClanManager().expulsarJugadorAsync(player, target, tUser);
            }
            return true;
        }

        if (subCommand.equals("disband")) {
            if (user.hasClan() && user.getClanRole().equals("LIDER")) {
                plugin.getClanManager().disolverClanAsync(player, user, user.getClanId());
            } else {
                player.sendMessage(NexoColor.parse(ERR_NO_PERM_LEADER));
            }
            return true;
        }

        if (subCommand.equals("deposit")) {
            if (!user.hasClan()) { player.sendMessage(NexoColor.parse(ERR_NO_CLAN)); return true; }
            if (args.length < 2) { player.sendMessage(NexoColor.parse(ERR_DEPOSIT_USAGE)); return true; }

            try {
                java.math.BigDecimal amount = new java.math.BigDecimal(args[1]);
                if (amount.compareTo(java.math.BigDecimal.ZERO) <= 0) {
                    player.sendMessage(NexoColor.parse(ERR_INVALID_AMOUNT)); return true;
                }

                me.nexo.economy.NexoEconomy eco = me.nexo.economy.NexoEconomy.getPlugin(me.nexo.economy.NexoEconomy.class);
                eco.getEconomyManager().updateBalanceAsync(player.getUniqueId(), me.nexo.economy.core.NexoAccount.AccountType.PLAYER, me.nexo.economy.core.NexoAccount.Currency.COINS, amount, false).thenAccept(success -> {
                    if (success) {
                        plugin.getClanManager().getClanFromCache(user.getClanId()).ifPresent(clan -> {
                            clan.depositMoney(amount.doubleValue());

                            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                                String sql = "UPDATE nexo_clans SET bank_balance = ? WHERE id = CAST(? AS UUID)";
                                try (java.sql.Connection conn = NexoCore.getPlugin(NexoCore.class).getDatabaseManager().getConnection();
                                     java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                                    ps.setBigDecimal(1, clan.getBankBalance());
                                    ps.setString(2, clan.getId().toString());
                                    ps.executeUpdate();
                                } catch (Exception e) {}
                            });
                            player.sendMessage(NexoColor.parse(MSG_DEPOSIT_SUCCESS.replace("%amount%", amount.toString())));
                        });
                    } else {
                        player.sendMessage(NexoColor.parse(ERR_INSUFFICIENT_FUNDS));
                    }
                });
            } catch (Exception e) { player.sendMessage(NexoColor.parse(ERR_INVALID_AMOUNT)); }
            return true;
        }

        if (subCommand.equals("withdraw")) {
            if (!user.hasClan()) { player.sendMessage(NexoColor.parse(ERR_NO_CLAN)); return true; }
            if (!user.getClanRole().equals("LIDER") && !user.getClanRole().equals("OFICIAL")) {
                player.sendMessage(NexoColor.parse(ERR_NO_PERM_OFFICIAL)); return true;
            }
            if (args.length < 2) { player.sendMessage(NexoColor.parse(ERR_WITHDRAW_USAGE)); return true; }

            try {
                java.math.BigDecimal amount = new java.math.BigDecimal(args[1]);
                if (amount.compareTo(java.math.BigDecimal.ZERO) <= 0) {
                    player.sendMessage(NexoColor.parse(ERR_INVALID_AMOUNT)); return true;
                }

                plugin.getClanManager().getClanFromCache(user.getClanId()).ifPresent(clan -> {
                    if (!clan.hasEnoughMoney(amount.doubleValue())) {
                        player.sendMessage(NexoColor.parse(ERR_CLAN_FUNDS)); return;
                    }

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

                    me.nexo.economy.NexoEconomy eco = me.nexo.economy.NexoEconomy.getPlugin(me.nexo.economy.NexoEconomy.class);
                    eco.getEconomyManager().updateBalanceAsync(player.getUniqueId(), me.nexo.economy.core.NexoAccount.AccountType.PLAYER, me.nexo.economy.core.NexoAccount.Currency.COINS, amount, true);

                    player.sendMessage(NexoColor.parse(MSG_WITHDRAW_SUCCESS.replace("%amount%", amount.toString())));
                });
            } catch (Exception e) { player.sendMessage(NexoColor.parse(ERR_INVALID_AMOUNT)); }
            return true;
        }

        if (subCommand.equals("sethome")) {
            if (!user.hasClan() || !user.getClanRole().equals("LIDER")) {
                player.sendMessage(NexoColor.parse(ERR_NO_PERM_LEADER)); return true;
            }
            plugin.getClanManager().getClanFromCache(user.getClanId()).ifPresent(clan -> {
                plugin.getClanManager().setClanHomeAsync(clan, player, player.getLocation());
            });
            return true;
        }

        if (subCommand.equals("home")) {
            if (!user.hasClan()) { player.sendMessage(NexoColor.parse(ERR_NO_CLAN)); return true; }
            plugin.getClanManager().getClanFromCache(user.getClanId()).ifPresent(clan -> {
                if (clan.getPublicHome() == null || clan.getPublicHome().isEmpty()) {
                    player.sendMessage(NexoColor.parse(ERR_NO_HOME));
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
                    player.sendMessage(NexoColor.parse(MSG_HOME_SUCCESS));
                } catch (Exception e) {
                    player.sendMessage(NexoColor.parse(ERR_HOME_COORD));
                }
            });
            return true;
        }

        if (subCommand.equals("tribute") || subCommand.equals("tributo")) {
            if (!user.hasClan()) { player.sendMessage(NexoColor.parse(ERR_NO_CLAN)); return true; }

            org.bukkit.inventory.ItemStack itemEnMano = player.getInventory().getItemInMainHand();
            if (itemEnMano == null || itemEnMano.getType() == org.bukkit.Material.AIR) {
                player.sendMessage(NexoColor.parse(ERR_NO_ITEM));
                return true;
            }

            long expPorItem = 1;
            String tipo = itemEnMano.getType().name();
            if (tipo.contains("DIAMOND") || tipo.contains("EMERALD")) expPorItem = 50;
            if (tipo.contains("IRON") || tipo.contains("GOLD")) expPorItem = 10;
            if (itemEnMano.hasItemMeta() && itemEnMano.getItemMeta().hasDisplayName()) expPorItem = 100;

            long expTotal = expPorItem * itemEnMano.getAmount();

            plugin.getClanManager().getClanFromCache(user.getClanId()).ifPresent(clan -> {
                boolean subioNivel = clan.addMonolithExp(expTotal);
                player.getInventory().setItemInMainHand(null);

                player.sendMessage(NexoColor.parse(MSG_TRIBUTE_SUCCESS.replace("%exp%", String.valueOf(expTotal))));

                if (subioNivel) {
                    // 🌟 Usamos Bukkit.broadcast() en lugar de broadcastMessage() para que acepte Components
                    Bukkit.broadcast(NexoColor.parse(BC_DIVIDER));
                    Bukkit.broadcast(NexoColor.parse(BC_LEVEL_UP_TITLE.replace("%clan%", clan.getName())));
                    Bukkit.broadcast(NexoColor.parse(BC_LEVEL_UP_DESC.replace("%level%", String.valueOf(clan.getMonolithLevel()))));
                    Bukkit.broadcast(NexoColor.parse(BC_DIVIDER));
                }

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

        player.sendMessage(NexoColor.parse(MSG_HELP));
        return true;
    }
}