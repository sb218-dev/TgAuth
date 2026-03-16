package ru.oleg.tgauth;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class AuthCommands implements CommandExecutor {
    private final TgAuthPlugin plugin;
    public AuthCommands(TgAuthPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (s instanceof Player p && l.equalsIgnoreCase("reg")) {
            String code = plugin.getDbManager().generateCode(p.getName());
            p.sendMessage("§6Твой код: §l" + code + "§e (напиши его боту)");
        }
        return true;
    }
}