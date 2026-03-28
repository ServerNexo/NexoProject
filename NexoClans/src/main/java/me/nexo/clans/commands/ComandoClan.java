package me.nexo.clans.commands;

import me.nexo.clans.NexoClans;
import me.nexo.clans.core.NexoClan;
import me.nexo.core.NexoCore;
import me.nexo.core.crossplay.CrossplayUtils;
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

        if (user == null) {
            CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.clan.errores.datos-cargando"));
            return true;
        }

        if (args.length == 0) {
            if (user.hasClan()) {
                Optional<NexoClan> clanOpt = plugin.getClanManager().getClanFromCache(user.getClanId());
                clanOpt.ifPresentOrElse(
                        clan -> me.nexo.clans.menu.ClanMenu.abrirMenu(player, clan, user),
                        () -> CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.clan.info.cargando"))
                );
            } else {
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.clan.errores.sin-clan"));
            }
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("create")) {
            if (user.hasClan()) {
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.clan.errores.ya-en-clan"));
                return true;
            }
            if (args.length < 3) {
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.clan.errores.uso-create"));
                return true;
            }
            String tag = args[1].toUpperCase();
            if (tag.length() < 2 || tag.length() > 4 || !tag.matches("^[A-Z0-9]+$")) {
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.clan.errores.tag-invalido"));
                return true;
            }
            String nombreRaw = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
            String nombreLimpio = nombreRaw.replaceAll("&#[a-fA-F0-9]{6}", "").replaceAll("&[0-9a-fk-orA-FK-OR]", "");
            if (nombreLimpio.length() < 3 || nombreLimpio.length() > 16) {
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.clan.errores.nombre-largo"));
                return true;
            }
            if (!nombreLimpio.matches("^[a-zA-Z0-9 ]+$")) {
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.clan.errores.nombre-invalido"));
                return true;
            }
            plugin.getClanManager().crearClanAsync(player, user, tag, nombreRaw);
            return true;
        }

        if (subCommand.equals("invite")) {
            if (!user.hasClan() || (!user.getClanRole().equals("LIDER") && !user.getClanRole().equals("OFICIAL"))) {
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.clan.errores.sin-permiso-oficial"));
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target != null)
                plugin.getClanManager().getClanFromCache(user.getClanId()).ifPresent(clan -> plugin.getClanManager().invitarJugador(player, target, clan));
            return true;
        }

        if (subCommand.equals("join")) {
            UUID inv = plugin.getClanManager().getInvitacionPendiente(player);
            if (inv != null)
                plugin.getClanManager().loadClanAsync(inv, clan -> plugin.getClanManager().unirseClanAsync(player, user, clan));
            return true;
        }

        if (subCommand.equals("leave")) {
            if (user.hasClan() && !user.getClanRole().equals("LIDER"))
                plugin.getClanManager().abandonarClanAsync(player, user);
            return true;
        }

        if (subCommand.equals("ff") || subCommand.equals("friendlyfire")) {
            if (user.hasClan() && (user.getClanRole().equals("LIDER") || user.getClanRole().equals("OFICIAL"))) {
                plugin.getClanManager().getClanFromCache(user.getClanId()).ifPresent(clan -> plugin.getClanManager().toggleFriendlyFireAsync(clan, player, !clan.isFriendlyFire()));
            } else {
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.clan.errores.sin-permiso-oficial"));
            }
            return true;
        }

        if (subCommand.equals("kick")) {
            if (!user.hasClan() || (!user.getClanRole().equals("LIDER") && !user.getClanRole().equals("OFICIAL"))) {
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.clan.errores.sin-permiso-oficial"));
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target != null) {
                NexoUser tUser = NexoCore.getPlugin(NexoCore.class).getUserManager().getUserOrNull(target.getUniqueId());
                if (tUser != null && tUser.getClanId().equals(user.getClanId()))
                    plugin.getClanManager().expulsarJugadorAsync(player, target, tUser);
            }
            return true;
        }

        if (subCommand.equals("disband")) {
            if (user.hasClan() && user.getClanRole().equals("LIDER")) {
                plugin.getClanManager().disolverClanAsync(player, user, user.getClanId());
            } else {
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.clan.errores.sin-permiso-lider"));
            }
            return true;
        }

        if (subCommand.equals("deposit")) {
            if (!user.hasClan()) {
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.clan.errores.sin-clan"));
                return true;
            }
            if (args.length < 2) {
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.clan.errores.uso-deposit"));
                return true;
            }
            try {
                java.math.BigDecimal amount = new java.math.BigDecimal(args[1]);
                if (amount.compareTo(java.math.BigDecimal.ZERO) <= 0) {
                    CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.clan.errores.cantidad-invalida"));
                    return true;
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
                                } catch (Exception e) {
                                }
                            });
                            CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.clan.exito.deposito").replace("%amount%", amount.toString()));
                        });
                    } else {
                        CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.clan.errores.fondos-insuficientes"));
                    }
                });
            } catch (Exception e) {
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.clan.errores.cantidad-invalida"));
            }
            return true;
        }

        if (subCommand.equals("withdraw")) {
            if (!user.hasClan()) {
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.clan.errores.sin-clan"));
                return true;
            }
            if (!user.getClanRole().equals("LIDER") && !user.getClanRole().equals("OFICIAL")) {
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.clan.errores.sin-permiso-oficial"));
                return true;
            }
            if (args.length < 2) {
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.clan.errores.uso-withdraw"));
                return true;
            }
            try {
                java.math.BigDecimal amount = new java.math.BigDecimal(args[1]);
                if (amount.compareTo(java.math.BigDecimal.ZERO) <= 0) {
                    CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.clan.errores.cantidad-invalida"));
                    return true;
                }
                plugin.getClanManager().getClanFromCache(user.getClanId()).ifPresent(clan -> {
                    if (!clan.hasEnoughMoney(amount.doubleValue())) {
                        CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.clan.errores.fondos-clan-insuficientes"));
                        return;
                    }
                    clan.withdrawMoney(amount.doubleValue());
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        String sql = "UPDATE nexo_clans SET bank_balance = ? WHERE id = CAST(? AS UUID)";
                        try (java.sql.Connection conn = NexoCore.getPlugin(NexoCore.class).getDatabaseManager().getConnection();
                             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                            ps.setBigDecimal(1, clan.getBankBalance());
                            ps.setString(2, clan.getId().toString());
                            ps.executeUpdate();
                        } catch (Exception e) {
                        }
                    });
                    me.nexo.economy.NexoEconomy eco = me.nexo.economy.NexoEconomy.getPlugin(me.nexo.economy.NexoEconomy.class);
                    eco.getEconomyManager().updateBalanceAsync(player.getUniqueId(), me.nexo.economy.core.NexoAccount.AccountType.PLAYER, me.nexo.economy.core.NexoAccount.Currency.COINS, amount, true);
                    CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.clan.exito.retiro").replace("%amount%", amount.toString()));
                });
            } catch (Exception e) {
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.clan.errores.cantidad-invalida"));
            }
            return true;
        }

        if (subCommand.equals("sethome")) {
            if (!user.hasClan() || !user.getClanRole().equals("LIDER")) {
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.clan.errores.sin-permiso-lider"));
                return true;
            }
            plugin.getClanManager().getClanFromCache(user.getClanId()).ifPresent(clan -> {
                plugin.getClanManager().setClanHomeAsync(clan, player, player.getLocation());
            });
            return true;
        }

        if (subCommand.equals("home")) {
            if (!user.hasClan()) {
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.clan.errores.sin-clan"));
                return true;
            }
            plugin.getClanManager().getClanFromCache(user.getClanId()).ifPresent(clan -> {
                if (clan.getPublicHome() == null || clan.getPublicHome().isEmpty()) {
                    CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.clan.errores.sin-home"));
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
                    CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.clan.exito.home"));
                } catch (Exception e) {
                    CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.clan.errores.home-invalido"));
                }
            });
            return true;
        }

        if (subCommand.equals("tribute") || subCommand.equals("tributo")) {
            if (!user.hasClan()) {
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.clan.errores.sin-clan"));
                return true;
            }
            org.bukkit.inventory.ItemStack itemEnMano = player.getInventory().getItemInMainHand();
            if (itemEnMano == null || itemEnMano.getType() == org.bukkit.Material.AIR) {
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.clan.errores.sin-item"));
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
                CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.clan.exito.tributo").replace("%exp%", String.valueOf(expTotal)));
                if (subioNivel) {
                    CrossplayUtils.broadcastMessage(plugin.getConfigManager().getMessage("comandos.clan.anuncios.level-up-divisor"));
                    CrossplayUtils.broadcastMessage(plugin.getConfigManager().getMessage("comandos.clan.anuncios.level-up-titulo").replace("%clan%", clan.getName()));
                    CrossplayUtils.broadcastMessage(plugin.getConfigManager().getMessage("comandos.clan.anuncios.level-up-desc").replace("%level%", String.valueOf(clan.getMonolithLevel())));
                    CrossplayUtils.broadcastMessage(plugin.getConfigManager().getMessage("comandos.clan.anuncios.level-up-divisor"));
                }
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    String sql = "UPDATE nexo_clans SET monolith_exp = ?, monolith_level = ? WHERE id = CAST(? AS UUID)";
                    try (java.sql.Connection conn = NexoCore.getPlugin(NexoCore.class).getDatabaseManager().getConnection();
                         java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setLong(1, clan.getMonolithExp());
                        ps.setInt(2, clan.getMonolithLevel());
                        ps.setString(3, clan.getId().toString());
                        ps.executeUpdate();
                    } catch (Exception e) {
                    }
                });
            });
            return true;
        }

        CrossplayUtils.sendMessage(player, plugin.getConfigManager().getMessage("comandos.clan.info.ayuda"));
        return true;
    }
}