package rip.diamond.hideandseek.game;

import me.goodestenglish.api.util.CC;
import me.goodestenglish.api.util.Common;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import rip.diamond.hideandseek.Config;
import rip.diamond.hideandseek.HideAndSeek;
import rip.diamond.hideandseek.Items;
import rip.diamond.hideandseek.disguise.Disguise;
import rip.diamond.hideandseek.disguise.impl.MiscDisguise;
import rip.diamond.hideandseek.enums.DisguiseTypes;
import rip.diamond.hideandseek.enums.GameRole;
import rip.diamond.hideandseek.enums.GameState;
import rip.diamond.hideandseek.event.GamePlayerDeathEvent;
import rip.diamond.hideandseek.player.GamePlayer;
import rip.diamond.hideandseek.util.PlayerUtil;
import rip.diamond.hideandseek.util.Util;

import java.util.UUID;

public class GameListener implements Listener {

    @EventHandler
    public void onLogin(AsyncPlayerPreLoginEvent event) {
        Game game = HideAndSeek.INSTANCE.getGame();

        if (game.isStarted()) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Common.text("<red>遊戲已開始, 無法進入"));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Game game = HideAndSeek.INSTANCE.getGame();

        game.getPlayers().putIfAbsent(player.getUniqueId(), new GamePlayer(player.getUniqueId()));

        PlayerUtil.reset(player);
        player.teleport(Config.LOBBY_LOCATION);

        event.joinMessage(Common.text("<gray>[<green>+<gray>] <yellow>" + player.getName()));
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Game game = HideAndSeek.INSTANCE.getGame();

        game.getPlayers().remove(player.getUniqueId());

        event.quitMessage(Common.text("<gray>[<red>-<gray>] <yellow>" + player.getName()));

        if (game.isStarted() && game.canEnd()) {
            game.end();
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (HideAndSeek.INSTANCE.getGame().getState() != GameState.SEEKER_PHASE) {
            event.setCancelled(true);
            return;
        }

        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK && event.getCause() != EntityDamageEvent.DamageCause.PROJECTILE) {
            event.setCancelled(true);
            return;
        }

        Entity entity = event.getEntity();
        Player player;

        if (entity instanceof Player) {
            player = (Player) entity;
        } else if (entity.hasMetadata(Game.DISGUISE_KEY)) {
            player = Bukkit.getPlayer((UUID) entity.getMetadata(Game.DISGUISE_KEY).getFirst().value());
        } else {
            player = null;
        }

        if (player == null) {
            return;
        }

        double health = player.getHealth();
        if (health - event.getDamage() > 0) {
            return;
        }
        GamePlayerDeathEvent e = new GamePlayerDeathEvent(player);
        e.callEvent();
        if (e.isCancelled()) {
            event.setDamage(0);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        Game game = HideAndSeek.INSTANCE.getGame();
        if (event.getEntity() instanceof Player entity && event.getDamager() instanceof Player damager) {
            GamePlayer gameEntity = game.getGamePlayer(entity);
            GamePlayer gameDamager = game.getGamePlayer(damager);

            if (gameEntity.getRole() == gameDamager.getRole()) {
                event.setCancelled(true);
                return;
            }

            if (gameEntity.getRole() == GameRole.SEEKER) {
                event.setDamage(0);
                return;
            }

            // TODO: 17/12/2022 Reveal block
        }
    }

    @EventHandler
    public void onDeath(GamePlayerDeathEvent event) {
        event.setCancelled(true);

        Game game = HideAndSeek.INSTANCE.getGame();
        Player player = event.getPlayer();
        GamePlayer gamePlayer = game.getGamePlayer(player);

        Common.broadcastMessage(gamePlayer.getRole().getColor() + player.getName() + "<red>已被擊殺，變成了" + GameRole.SEEKER.getColoredName());

        gamePlayer.getDisguises().getDisguise().stopDisguise();
        gamePlayer.setRole(GameRole.SEEKER);
        game.getMap().teleport(player);
        player.getInventory().setContents(GameRole.SEEKER.getTools());

        Common.sendMessage(player, "","<yellow>基於你已被" + GameRole.SEEKER.getColoredName() + "<yellow>發現，你現在是" + gamePlayer.getRole().getColoredName() + "<yellow>!");
        Common.sendMessage(player, "<yellow>獲勝目標: <dark_aqua>" + gamePlayer.getRole().getGoal());

        if (game.canEnd()) {
            game.end();
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        if (event.getEntityType() == EntityType.ARROW) {
            projectile.remove();
        }
    }

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onLoad(ChunkLoadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof Player) {
                continue;
            }
            entity.remove();
        }
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        event.getWorld().setDifficulty(Difficulty.HARD);
    }

    @EventHandler
    public void onSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CUSTOM && event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.DEFAULT) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.CREATIVE) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.CREATIVE) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (player.getGameMode() != GameMode.CREATIVE) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDrop(EntityDropItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (player.getGameMode() != GameMode.CREATIVE) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.CREATIVE) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (player.getGameMode() != GameMode.CREATIVE) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();
        ItemStack itemStack = event.getItem();
        Block block = event.getClickedBlock();
        Game game = HideAndSeek.INSTANCE.getGame();
        GamePlayer gamePlayer = game.getGamePlayer(player);

        if (player.getGameMode() != GameMode.CREATIVE && action == Action.RIGHT_CLICK_BLOCK && block != null && block.getType() == Material.CHEST) {
            event.setCancelled(true);
            return;
        }

        if (game.isStarted()) {
            if (game.getGamePlayer(player).getRole() == GameRole.HIDER) {
                if (itemStack != null) {
                    if (itemStack.equals(Items.TRANSFORM_TOOL.getItem()) && block != null && block.getType() != Material.AIR && block.getType() != Material.BARRIER) {
                        BoundingBox box = block.getBoundingBox();

                        if (box.getVolume() != 1) {
                            Common.sendMessage(player, "<red>你只能變身成為一個完整的方塊!");
                            return;
                        }

                        gamePlayer.getDisguises().getDisguise().stopDisguise();
                        gamePlayer.getDisguises().setType(DisguiseTypes.BLOCK);
                        gamePlayer.getDisguises().setData(block.getType().name());
                        gamePlayer.getDisguises().setDisguise(Util.disguise(player));
                        Common.sendMessage(player, "<yellow>你已變身成為 <aqua>" + block.getType().name());
                    } else if (itemStack.equals(Items.TELEPORT_TOOL.getItem())) {
                        Location blockLoc = player.getLocation().getBlock().getLocation().clone();
                        player.teleport(new Location(blockLoc.getWorld(), blockLoc.getX() + 0.5, blockLoc.getY(), blockLoc.getZ() + 0.5, player.getLocation().getYaw(), player.getLocation().getPitch()));
                        Common.sendMessage(player, "<yellow>已成功固定!");
                    } else if (itemStack.equals(Items.DIRECTION_CHANGE.getItem())) {
                        MiscDisguise disguise = (MiscDisguise) gamePlayer.getDisguises().getDisguise();

                        if (disguise.getBlockDisplay().getBlock() instanceof Directional directional) {
                            int index = directional.getFacing().ordinal();
                            BlockFace candidate;

                            do {
                                index = (index + 1) % BlockFace.values().length;
                                candidate = BlockFace.values()[index];
                            } while (!directional.getFaces().contains(candidate));

                            directional.setFacing(candidate);
                            disguise.getBlockDisplay().setBlock(directional);
                            Common.sendMessage(player, "<yellow>新的方向已變為 <aqua>" + candidate.name());
                        } else {
                            Common.sendMessage(player, "<red>這方塊不支援改變方向");
                        }
                    }
                }
            }
        }
    }

}
