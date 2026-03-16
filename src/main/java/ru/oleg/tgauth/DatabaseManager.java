package ru.oleg.tgauth;

import java.io.File;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class DatabaseManager {
    private Connection connection;
    private final Map<String, String> tempCodes = new HashMap<>();
    private final Map<String, Long> expiryTimes = new HashMap<>();

    public DatabaseManager(TgAuthPlugin plugin) {
        try {
            File dbFile = new File(plugin.getDataFolder(), "database.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            connection.createStatement().execute("CREATE TABLE IF NOT EXISTS players (player_name TEXT PRIMARY KEY, tg_chat_id BIGINT)");
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public String generateCode(String playerName) {
        String code = String.format("%05d", new Random().nextInt(100000));
        tempCodes.put(playerName, code);
        expiryTimes.put(playerName, System.currentTimeMillis() + 120000); // 2 мин
        return code;
    }

    public String getPlayerByCode(String code) {
        for (String name : tempCodes.keySet()) {
            if (tempCodes.get(name).equals(code) && System.currentTimeMillis() < expiryTimes.get(name)) {
                tempCodes.remove(name);
                return name;
            }
        }
        return null;
    }

    public void linkTelegram(String name, long chatId) {
        try (PreparedStatement ps = connection.prepareStatement("INSERT OR REPLACE INTO players VALUES (?, ?)")) {
            ps.setString(1, name);
            ps.setLong(2, chatId);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public Long getChatId(String name) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT tg_chat_id FROM players WHERE player_name = ?")) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getLong("tg_chat_id");
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public void close() { try { if (connection != null) connection.close(); } catch (SQLException e) { e.printStackTrace(); } }
}