package ru.oleg.tgauth;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuthListener implements Listener {
    private final Map<UUID, Location> unauthPlayers = new HashMap<>();
    private final TgAuthPlugin plugin;

    public AuthListener(TgAuthPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();

        // Регистрируем локацию входа для ВСЕХ зашедших
        unauthPlayers.put(p.getUniqueId(), p.getLocation());

        // Накладываем слепоту
        p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 0));

        Long chatId = plugin.getDbManager().getChatId(p.getName());
        if (chatId != null) {
            p.sendMessage("§6Запрос отправлен в Telegram...");
            plugin.getBot().sendAuthRequest(p.getName(), chatId);
        } else {
            p.sendMessage("§eВведите §l/reg§e для привязки Telegram.");
        }

        // Авто-кик через 2 минуты
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (unauthPlayers.containsKey(p.getUniqueId())) {
                p.kickPlayer("§cВремя авторизации истекло!");
            }
        }, 2400L);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (unauthPlayers.containsKey(p.getUniqueId())) {
            Location loc = unauthPlayers.get(p.getUniqueId());
            // Если игрок сдвинулся, возвращаем на место, но даем крутить головой
            if (e.getFrom().distanceSquared(loc) > 0.01) {
                Location back = loc.clone();
                back.setPitch(e.getTo().getPitch());
                back.setYaw(e.getTo().getYaw());
                e.setTo(back);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        unauthPlayers.remove(e.getPlayer().getUniqueId());
    }

    public void authenticate(Player p) {
        if (p == null) return;

        // Удаляем из списка блокировки
        unauthPlayers.remove(p.getUniqueId());

        plugin.getLogger().info("[DEBUG] Разблокировка игрока: " + p.getName());

        // Выполняем действия в основном потоке сервера
        Bukkit.getScheduler().runTask(plugin, () -> {
            p.removePotionEffect(PotionEffectType.BLINDNESS);

            // Микро-задержка для обновления позиции клиента
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (p.isOnline()) {
                    p.teleport(p.getLocation());
                    p.setWalkSpeed(0.2f);
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

                    String rawMsg = plugin.getConfig().getString("welcome-message", "&aВы успешно авторизовались!");
                    String formatted = ChatColor.translateAlternateColorCodes('&', rawMsg.replace("{player}", p.getName()));
                    p.sendMessage(formatted);

                    plugin.getLogger().info("[DEBUG] " + p.getName() + " успешно разморожен!");
                }
            }, 5L);
        });
    }
}