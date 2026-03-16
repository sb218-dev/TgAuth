package ru.oleg.tgauth;

import org.bukkit.plugin.java.JavaPlugin;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class TgAuthPlugin extends JavaPlugin {
    private DatabaseManager dbManager;
    private AuthListener authListener;
    private TgBot bot;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        dbManager = new DatabaseManager(this);
        authListener = new AuthListener(this);
        getServer().getPluginManager().registerEvents(authListener, this);

        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            bot = new TgBot(this);
            botsApi.registerBot(bot);
            getLogger().info("Telegram-бот запущен!");
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

        getCommand("reg").setExecutor(new AuthCommands(this));
    }

    @Override
    public void onDisable() {
        if (dbManager != null) dbManager.close();
    }

    public DatabaseManager getDbManager() { return dbManager; }
    public AuthListener getAuthListener() { return authListener; }
    public TgBot getBot() { return bot; }
}