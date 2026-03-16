package ru.oleg.tgauth;

import org.bukkit.plugin.java.JavaPlugin;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import java.net.Authenticator;
import java.net.PasswordAuthentication;

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
            
            DefaultBotOptions options = new DefaultBotOptions();
            
            // Настройка SOCKS/HTTP прокси
            if (getConfig().getBoolean("proxy.enabled", false)) {
                String type = getConfig().getString("proxy.type", "HTTP").toUpperCase();
                String host = getConfig().getString("proxy.host", "127.0.0.1");
                int port = getConfig().getInt("proxy.port", 1080);
                
                options.setProxyHost(host);
                options.setProxyPort(port);
                
                switch (type) {
                    case "SOCKS4": options.setProxyType(DefaultBotOptions.ProxyType.SOCKS4); break;
                    case "SOCKS5": options.setProxyType(DefaultBotOptions.ProxyType.SOCKS5); break;
                    default: options.setProxyType(DefaultBotOptions.ProxyType.HTTP); break;
                }

                String proxyUser = getConfig().getString("proxy.username", "");
                String proxyPass = getConfig().getString("proxy.password", "");
                
                if (!proxyUser.isEmpty() && !proxyPass.isEmpty()) {
                    Authenticator.setDefault(new Authenticator() {
                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                            if (getRequestingHost().equalsIgnoreCase(host) && getRequestingPort() == port) {
                                return new PasswordAuthentication(proxyUser, proxyPass.toCharArray());
                            }
                            return super.getPasswordAuthentication();
                        }
                    });
                }
                
                getLogger().info("Используется прокси: " + type + " " + host + ":" + port);
            }
            
            // Настройка Reverse Proxy (кастомный URL API)
            if (getConfig().getBoolean("proxy.use-reverse-proxy", false)) {
                String customUrl = getConfig().getString("proxy.reverse-proxy-url");
                if (customUrl != null && !customUrl.isEmpty()) {
                    options.setBaseUrl(customUrl);
                    getLogger().info("Используется кастомный API URL: " + customUrl);
                }
            }

            bot = new TgBot(this, options);
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