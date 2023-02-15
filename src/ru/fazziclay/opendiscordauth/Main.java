package ru.fazziclay.opendiscordauth;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;

import java.awt.*;

public class Main extends JavaPlugin {

    public static FileConfiguration pluginConfig;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!Config.isDebugEnable) {
            return true;
        }

        if (command.getName().equalsIgnoreCase("o")) {
            if (args.length == 0) {
                sender.sendMessage("Комманда для debugging`a плагина OpenDiscordAuth");
                return true;
            }

            if (args[0].equalsIgnoreCase("accounts")) {
                sender.sendMessage(Account.accounts.toString());
            }
        }
        return true;
    }

    @Override
    public void onEnable() {
        loadConfig();

        Utils.debug("[Main] onEnable()");
        try {
            loadDiscordBot();
            loadAccounts();
            UpdateChecker.loadUpdateChecker();
            Bukkit.getPluginManager().registerEvents(new ServerEvents(), this);

        } catch (Exception e) {
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        Utils.debug("[Main] onDisable()");
        LoginManager.kickAllNotAuthorizedPlayers();
        DiscordBot.bot.shutdownNow();
    }

    public void loadConfig() {
        getConfig().options().copyDefaults(true);
        saveConfig();
        Main.pluginConfig = getConfig();
        Utils.debug("[Main] loadConfig(): loaded!");
    }

    private static void loadDiscordBot() throws Exception { // Загрузка бота
        Utils.debug("[Main] loadDiscordBot()");

        DiscordBot.bot = JDABuilder.createDefault(Config.discordBotToken)
            .setChunkingFilter(ChunkingFilter.ALL)
            .setMemberCachePolicy(MemberCachePolicy.ALL)
            .enableIntents(GatewayIntent.GUILD_PRESENCES)
            .enableIntents(GatewayIntent.GUILD_MEMBERS)
            .addEventListeners(new DiscordBot())
            .build();

        DiscordBot.bot.awaitReady();
    }

    private void loadAccounts() throws JSONException { // Загрузка аккаунтов
        if (!Utils.isFileExist(Config.accountsFilePath)) {
            Utils.writeFile(Config.accountsFilePath, "[]");
        }
        Account.accountsJson = new JSONArray(Utils.readFile(Config.accountsFilePath));

        int i = 0;
        while (i < Account.accountsJson.length()) {
            String discord  = Account.accountsJson.getJSONObject(i).getString("discord");
            String nickname = Account.accountsJson.getJSONObject(i).getString("nickname");

            if (!Account.accountsJson.getJSONObject(i).has("effectiveNick")) Account.accountsForceRewrite = true;

            String effectiveNick =
                Account.accountsJson.getJSONObject(i).has("effectiveNick")
                ? Account.accountsJson.getJSONObject(i).getString("effectiveNick")
                : DiscordBot.getMember(discord).getEffectiveName();

            String effectiveAvatarUrl =
                Account.accountsJson.getJSONObject(i).has("effectiveAvatarUrl")
                ? Account.accountsJson.getJSONObject(i).getString("effectiveAvatarUrl")
                : DiscordBot.getMember(discord).getEffectiveAvatarUrl();

            String guildColor =
                Account.accountsJson.getJSONObject(i).has("guildColor")
                ? Account.accountsJson.getJSONObject(i).getString("guildColor")
                : Utils.getMemberHexColor(DiscordBot.getMember(discord));

            Account account = new Account(discord, nickname, false, effectiveNick, effectiveAvatarUrl, guildColor);
            Account.accounts.add(account);

            i++;
        }

        if (Account.accountsForceRewrite) {
            Account.rewriteAccounts();
        }

    }
}
