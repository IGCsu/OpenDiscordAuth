package ru.fazziclay.opendiscordauth;

import net.kyori.adventure.text.TranslatableComponent;
import org.bukkit.GameMode;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import de.jeter.chatex.api.events.PlayerUsesGlobalChatEvent;
import ru.fazziclay.opendiscordauth.discordbot.DiscordBot;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.Objects;

public class ServerEvents implements Listener {
    @EventHandler
    public void onLogin(PlayerLoginEvent event) {
        Utils.debug("[ServerEvents] onLogin(-)");

        String uuid = event.getPlayer().getUniqueId().toString();

        if (!LoginManager.notAuthorizedPlayers.contains(uuid)) {
            LoginManager.notAuthorizedPlayers.add(uuid);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) throws InterruptedException {
        Utils.debug("[ServerEvents] onPlayerJoin(-)");

        Player player = event.getPlayer();
        String uuid = player.getUniqueId().toString();
        String nickname = player.getName();
        String ip = Utils.getPlayerIp(player);

        if (Session.isActive(nickname, ip)) {
            LoginManager.login(uuid);

            Session session = Session.getByValue(0, nickname);
            assert session != null;
            session.delete();

            return;
        }

        if (Account.getByValue(0, nickname) == null) {
            Utils.sendMessage(player, Config.messageRegistrationInstructions);

        } else {
            Utils.sendMessage(player, Config.messageLoginInstructions);
        }

        player.setGameMode(GameMode.SPECTATOR);
        LoginManager.giveCode(uuid, nickname, player);
        String displayName = event.getPlayer().getDisplayName().replaceAll("§.", "");
        DiscordBot.webhook.sendMessage(Config.messagePlayerJoined.replace("$discordname", displayName));
        DiscordBot.updateOnlineStatus(player, true);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) throws InterruptedException {
        Utils.debug("[ServerEvents] onPlayerQuit(-)");

        Player player = event.getPlayer();
        String uuid = player.getUniqueId().toString();
        String nickname = player.getName();
        String displayName = event.getPlayer().getDisplayName().replaceAll("§.", "");
        String ip = Utils.getPlayerIp(player);

        TempCode tempCode = TempCode.getByValue(2, nickname);
        if (tempCode != null) tempCode.delete(); // Если для этого игрока есть сгенерированный код то удалить его.

        Account account = Account.getByValue(0, nickname);
        if (account != null && account.temp) account.delete(); // Удалить аккаунт если он временный

        if (LoginManager.isAuthorized(uuid)) {
            Session.update(nickname, ip);
        }
        LoginManager.notAuthorizedPlayers.remove(uuid);
        DiscordBot.webhook.sendMessage(Config.messagePlayerLeft.replace("$discordname", displayName));
        DiscordBot.updateOnlineStatus(player, false);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Utils.debug("[ServerEvents] onPlayerChat(...)");

        Player player = event.getPlayer();
        String uuid = player.getUniqueId().toString();
        String nickname = player.getName();
        String content = event.getMessage();

        Utils.debug("[ServerEvents] onPlayerChat(...): nickname="+nickname+"; content="+content);

        if (!LoginManager.isAuthorized(uuid)) {
            Utils.debug("[ServerEvents] onPlayerChat(...): not authorized!");

            Account account = Account.getByValue(0, nickname);
            if (account != null && account.temp) {
                Utils.debug("[ServerEvents] onPlayerChat(...): Detected temp account");

                if (content.equals(Config.commandConfirm)) {
                    account.makePermanent();
                    LoginManager.login(uuid);

                } else if (content.equals(Config.commandCancel)) {
                    account.delete();
                    LoginManager.giveCode(uuid, nickname, player);
                }
            }

            event.setCancelled(true);
        }
    }

    @EventHandler
    public void PlayerUsesGlobalChatEvent (PlayerUsesGlobalChatEvent event) throws InterruptedException {
        String message = event.getMessage();
        Player player = event.getPlayer();
        Account account = Objects.requireNonNull(Account.getByValue(0, player.getName()));
        String name = account.effectiveNick;

        if (Config.chatPrefix != null) {
            if (!Config.chatPrefix.startsWith("ENTER")) {
                name = String.format("[%s] %s", Config.chatPrefix, account.effectiveNick);
            }
        }

        DiscordBot.webhook.sendMessage(
            message,
            name,
            account.effectiveAvatarUrl
        );
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) { // Отмена движения игрока
        String uuid = event.getPlayer().getUniqueId().toString();

        if (!LoginManager.isAuthorized(uuid)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onTarget(EntityTargetEvent event) {
        if (event.getTarget() instanceof Player player) {
            boolean isAuthorized = LoginManager.isAuthorized(player.getUniqueId().toString());
            if (!isAuthorized) event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntityType() == EntityType.PLAYER) {
            boolean isAuthorized = LoginManager.isAuthorized(event.getEntity().getUniqueId().toString());
            if (!isAuthorized) event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerAdvancementDoneEvent(PlayerAdvancementDoneEvent event) throws InterruptedException {
        io.papermc.paper.advancement.AdvancementDisplay advancementInfo = event.getAdvancement().getDisplay();
        if (Objects.isNull(advancementInfo)) return;
        if (advancementInfo.doesAnnounceToChat()) {
            String title = PlainTextComponentSerializer.plainText().serialize(advancementInfo.title());
            String description = PlainTextComponentSerializer.plainText().serialize(advancementInfo.description());
            String displayName = event.getPlayer().getDisplayName().replaceAll("§.", "");

            DiscordBot.webhook.sendMessage(
                    Config.messagePlayerAchievementReceive
                            .replace("$discordname", displayName)
                            .replace("$achievementname", title)
                            .replace("$achievementdescription", description)
            );
        }
    }

    @EventHandler
    public void onPlayerDeathEvent(PlayerDeathEvent event) throws InterruptedException {
        String deathMessage = PlainTextComponentSerializer.plainText().serialize(event.deathMessage());
        DiscordBot.webhook.sendMessage(deathMessage);
    }
}
