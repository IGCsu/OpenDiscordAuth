package ru.fazziclay.opendiscordauth;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import org.bukkit.Bukkit;
import org.json.JSONArray;
import org.json.JSONObject;
import ru.fazziclay.opendiscordauth.discordbot.DiscordBot;

import java.util.ArrayList;
import java.util.List;


public class Account {

    // Static
    public static List<Account> accounts = new ArrayList<>();
    public static JSONArray     accountsJson = new JSONArray();


    public static void makePermanent(Account account) {
        Utils.debug("[Account] makePermanent()");

        if (account.temp) {
            account.temp = false;

            Account.accountsJson.put(account.toJSON());

            Utils.writeFile(Config.accountsFilePath, accountsJson.toString(4));
        }
    }

    public static void delete(Account account) {
        Utils.debug("[Account] delete()");

        // remove
        Account.accounts.remove(account);
        int i=0;
        while (i < accountsJson.length()) {
            JSONObject a = Account.accountsJson.getJSONObject(i);
            if ( (a.getString("discord").equals(account.ownerDiscord)) && (a.getString("nickname").equals(account.ownerNickname)) ) {
                Account.accountsJson.remove(i);
                break;
            }

            i++;
        }
        // remove
    }

    public static void create(User ownerDiscord, String ownerNickname) {
        Utils.debug("[Account] create()");

        String discordNickname = ownerDiscord.getAsTag();
        Utils.sendMessage(Bukkit.getPlayer(ownerNickname), Config.messageAccountCreatingConfirming.replace("$discord", discordNickname).replace("$nickname", ownerNickname));

        Account account = new Account(ownerDiscord.getId(), ownerNickname, true);

        Account.accounts.add(account);
    }

    public static Account getByValue(int type, Object value) {
        Utils.debug("[Account] getByValue("+type+", "+value+")");

        int i = 0;
        while (i < accounts.size()) {
            Account currentAccount = accounts.get(i);

            if (type == 0 && currentAccount.ownerNickname.equals(value)) {
                Utils.debug("[Account] getByValue("+type+", "+value+"): returned '"+currentAccount+"'");
                return currentAccount;
            }

            if (type == 2 && currentAccount.ownerDiscord.equals(value)) {
                Utils.debug("[Account] getByValue("+type+", "+value+"): returned '"+currentAccount+"'");
                return currentAccount;
            }

            if (type == 3 && currentAccount.temp == (boolean) value) {
                Utils.debug("[Account] getByValue("+type+", "+value+"): returned '"+currentAccount+"'");
                return currentAccount;
            }


            i++;
        }

        Utils.debug("[Account] getByValue("+type+", "+value+"): returned 'null'");
        return null;
    }


    //Not-static
    public String ownerDiscord;
    public String ownerNickname;
    public boolean temp;
    public String effectiveNick;
    public String effectiveAvatarUrl;
    public String guildColor;

    public void makePermanent() {
        Utils.debug("[Account] [object] makePermanent()");
        Account.makePermanent(this);
    }

    public void delete() {
        Utils.debug("[Account] [object] delete()");
        Account.delete(this);
    }

    public void addMemberData() {
        Utils.debug("[Account] [object] update()");
        Account.addMemberData(this);
    }

    public static void addMemberData(Account account) {
        Member member = DiscordBot.getMember(account.ownerDiscord);
        int i = Account.accounts.indexOf(account);
        account.effectiveNick = member.getEffectiveName();
        account.effectiveAvatarUrl = member.getEffectiveAvatarUrl();
        account.guildColor = Utils.getMemberHexColor(member);
        Account.accounts.set(i, account);
    }

    public static JSONObject toJSON(Account account) {
        JSONObject jsonAccount = new JSONObject();
        jsonAccount.put("discord", account.ownerDiscord);
        jsonAccount.put("nickname", account.ownerNickname);
        return jsonAccount;
    }

    public JSONObject toJSON() {
        return Account.toJSON(this);
    }

    @Override
    public String toString() {
        return this.ownerDiscord + " "
            + this.ownerNickname + " "
            + this.effectiveNick + " "
            + this.effectiveAvatarUrl + " "
            + this.guildColor;
    }

    // Constructor
    public Account(String ownerDiscord, String ownerNickname, boolean temp) {
        Utils.debug("[Account] -> created new object: (ownerDiscord="+ownerDiscord+"; ownerNickname="+ownerNickname+"; temp="+temp+")");

        this.ownerDiscord = ownerDiscord;
        this.ownerNickname = ownerNickname;
        this.temp = temp;
    }
}
